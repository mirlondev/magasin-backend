package org.odema.posnew.application.serviceImpl;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.api.exception.UnauthorizedException;
import org.odema.posnew.application.dto.request.OrderRequest;
import org.odema.posnew.application.dto.request.PaymentRequest;
import org.odema.posnew.application.dto.response.OrderResponse;
import org.odema.posnew.application.mapper.OrderMapper;
import org.odema.posnew.design.event.*;
import org.odema.posnew.design.factory.SaleStrategyFactory;
import org.odema.posnew.design.handler.PaymentHandler;
import org.odema.posnew.design.template.OrderServiceTemplate;
import org.odema.posnew.domain.model.*;
import org.odema.posnew.domain.model.enums.OrderStatus;
import org.odema.posnew.domain.model.enums.PaymentMethod;
import org.odema.posnew.domain.model.enums.PaymentStatus;
import org.odema.posnew.domain.repository.*;
import org.odema.posnew.domain.service.OrderService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrderServiceImpl extends OrderServiceTemplate implements OrderService {

    private final PaymentRepository paymentRepository;
    private final ShiftReportRepository shiftReportRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository,
            StoreRepository storeRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            OrderMapper orderMapper,
            SaleStrategyFactory strategyFactory,
            PaymentRepository paymentRepository,
            ShiftReportRepository shiftReportRepository,
            @Qualifier("cashPaymentHandler") PaymentHandler cashHandler,
            @Qualifier("creditCardPaymentHandler") PaymentHandler cardHandler,
            @Qualifier("mobileMoneyPaymentHandler") PaymentHandler mobileHandler,
            @Qualifier("creditPaymentHandler") PaymentHandler creditHandler,
            ApplicationEventPublisher eventPublisher
    ) {
        super(orderRepository, orderItemRepository, customerRepository,
                userRepository, storeRepository, productRepository,
                inventoryRepository, orderMapper, strategyFactory);

        this.paymentRepository = paymentRepository;
        this.shiftReportRepository = shiftReportRepository;
        this.eventPublisher = eventPublisher;

        // Chaîne de responsabilité pour les handlers de paiement
        cashHandler.setNext(cardHandler);
        cardHandler.setNext(mobileHandler);
        mobileHandler.setNext(creditHandler);
    }

    // =========================================================================
    // HOOK — Override du template pour publier l'événement post-création
    // =========================================================================

    @Override
    protected void afterOrderSaved(Order savedOrder, OrderRequest request) {
        eventPublisher.publishEvent(new OrderCreatedEvent(this, savedOrder));
    }

    // =========================================================================
    // ORDER CREATION — délègue entièrement au template
    // =========================================================================

    /**
     * Crée une commande sans paiement.
     * Toute la logique (validation, construction, stock, stats) est dans
     * OrderServiceTemplate.createOrder() — on ne fait qu'appeler super.
     */
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, UUID cashierId) {
        return super.createOrder(request, cashierId);
    }

    /**
     * Crée une commande puis y attache immédiatement un premier paiement.
     */
    @Override
    @Transactional
    public OrderResponse createOrderWithPayment(OrderRequest orderRequest,
                                                PaymentRequest paymentRequest,
                                                UUID cashierId) throws UnauthorizedException {
        OrderResponse orderResponse = createOrder(orderRequest, cashierId);

        if (paymentRequest != null
                && paymentRequest.amount().compareTo(BigDecimal.ZERO) > 0) {
            return addPaymentToOrder(orderResponse.orderId(), paymentRequest, cashierId);
        }

        return orderResponse;
    }

    // =========================================================================
    // PAYMENT HANDLING
    // =========================================================================

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse addPaymentToOrder(UUID orderId,
                                           PaymentRequest paymentRequest,
                                           UUID cashierId) throws UnauthorizedException {

        // ✅ Validation montant
        if (paymentRequest.amount() == null
                || paymentRequest.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Le montant du paiement doit être positif");
        }

        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.warn("Tentative paiement sur commande annulée: {}", orderId);
            throw new BadRequestException(
                    "Impossible d'ajouter un paiement à une commande annulée");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("La commande est déjà complétée");
        }

        // ✅ Vérifier que le paiement ne dépasse pas 150% du montant restant
        // (tolérance pour les arrondis monnaie)
        if (paymentRequest.method() != PaymentMethod.CREDIT) {
            BigDecimal remaining = order.getRemainingAmount();
            BigDecimal maxAllowed = remaining.multiply(BigDecimal.valueOf(1.5))
                    .max(BigDecimal.valueOf(50000)); // minimum 50000 FCFA de tolérance
            if (paymentRequest.amount().compareTo(maxAllowed) > 0) {
                throw new BadRequestException(String.format(
                        "Montant trop élevé. Restant dû: %.2f FCFA", remaining));
            }
        }

        User cashier = loadCashier(cashierId);

        // ✅ Vérifier que le caissier est actif
        if (!cashier.getActive()) {
            throw new BadRequestException("Ce compte caissier est désactivé");
        }

        ShiftReport shiftReport = shiftReportRepository
                .findOpenShiftByCashier(cashierId)
                .orElseThrow(() -> new BadRequestException(
                        "Aucune caisse ouverte pour ce caissier"));

        Payment payment = buildAndSavePayment(paymentRequest, order, cashier, shiftReport);
        order.addPayment(payment);

        if (payment.isActualPayment()) {
            shiftReport.addSale(payment.getAmount(), payment.getMethod());
            shiftReportRepository.save(shiftReport);
        }

        Order updatedOrder = orderRepository.save(order);
        eventPublisher.publishEvent(new PaymentReceivedEvent(this, updatedOrder, payment));

        log.debug("Paiement ajouté - Statut: {}, Total payé: {}",
                updatedOrder.getPaymentStatus(), updatedOrder.getTotalPaid());

        return orderMapper.toResponse(updatedOrder);
    }

    private Payment buildAndSavePayment(PaymentRequest request, Order order,
                                        User cashier, ShiftReport shiftReport) {
        PaymentStatus status = request.method() == PaymentMethod.CREDIT
                ? PaymentStatus.CREDIT
                : PaymentStatus.PAID;

        log.info("Création paiement - Méthode: {}, Montant: {}, Statut: {}",
                request.method(), request.amount(), status);

        Payment payment = Payment.builder()
                .order(order)
                .method(request.method())
                .amount(request.amount())
                .cashier(cashier)
                .shiftReport(shiftReport)
                .status(status)
                .notes(request.notes())
                .isActive(true)
                .build();

        return paymentRepository.save(payment);
    }

    // =========================================================================
    // ORDER LIFECYCLE
    // =========================================================================

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse markAsCompleted(UUID orderId) {
        log.info("Clôture commande {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible de terminer une commande annulée");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            return orderMapper.toResponse(order);
        }
        if (order.getPaymentStatus() == PaymentStatus.UNPAID) {
            throw new BadRequestException("La commande n'a aucun paiement enregistré");
        }

        // markAsCompleted() appelle customer.recordPurchase() en interne si client présent
        order.markAsCompleted();
        Order updatedOrder = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderCompletedEvent(this, updatedOrder));

        return orderMapper.toResponse(updatedOrder);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN')")
    public void cancelOrder(UUID orderId) {
        log.info("Annulation commande {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("La commande est déjà annulée");
        }
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Impossible d'annuler une commande terminée");
        }

        // cancel() existe dans Order avec une signature (String reason)
        order.cancel("Annulée par l'opérateur");
        orderRepository.save(order);

        // restoreInventoryForOrder est protected dans le template — accessible ici
        restoreInventoryForOrder(order);

        eventPublisher.publishEvent(new OrderCancelledEvent(this, order));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse updateOrder(UUID orderId, OrderRequest request) {
        log.info("Mise à jour commande {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        if (order.getStatus() == OrderStatus.COMPLETED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException(
                    "Impossible de modifier une commande " + order.getStatus());
        }

        if (request.customerId() != null) {
            Customer customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new NotFoundException("Client non trouvé"));
            order.setCustomer(customer);
        }

        if (request.notes() != null)
            order.setNotes(request.notes());

        // Remise globale — champs réels de Order (pas discountAmount ni taxRate)
        if (request.discountAmount() != null)
            order.setGlobalDiscountAmount(request.discountAmount());
        if (request.globalDiscountPercentage() != null)
            order.setGlobalDiscountPercentage(request.globalDiscountPercentage());

        // Pas de calculateTotals() — Order calcule tout en @Transient à la volée
        return orderMapper.toResponse(orderRepository.save(order));
    }

    // =========================================================================
    // QUERIES
    // =========================================================================

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse getOrderById(UUID orderId) {
        return orderMapper.toResponse(
                orderRepository.findByIdWithPayments(orderId)
                        .orElseThrow(() -> new NotFoundException("Commande non trouvée")));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse getOrderByNumber(String orderNumber) {
        return orderMapper.toResponse(
                orderRepository.findByOrderNumber(orderNumber)
                        .orElseThrow(() -> new NotFoundException("Commande non trouvée")));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public Page<OrderResponse> getOrders(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

        return switch (user.getUserRole()) {
            case ADMIN -> orderRepository.findAll(pageable)
                    .map(orderMapper::toResponse);
            case STORE_ADMIN -> orderRepository
                    .findByStore_StoreId(user.getAssignedStore().getStoreId(), pageable)
                    .map(orderMapper::toResponse);
            case CASHIER -> orderRepository
                    .findByCashier_UserId(user.getUserId(), pageable)
                    .map(orderMapper::toResponse);
            default -> throw new BadRequestException("Rôle non autorisé: " + user.getUserRole());
        };
    }

    @Override
    public List<OrderResponse> getOrdersByStore(UUID storeId) {
        return orderRepository.findByStore_StoreId(storeId).stream()
                .map(orderMapper::toResponse).toList();
    }

    @Override
    public List<OrderResponse> getOrdersByCustomer(UUID customerId) {
        return orderRepository.findByCustomer_CustomerId(customerId).stream()
                .map(orderMapper::toResponse).toList();
    }

    @Override
    public List<OrderResponse> getOrdersByCashier(UUID cashierId) {
        return orderRepository.findByCashier_UserId(cashierId).stream()
                .map(orderMapper::toResponse).toList();
    }

    @Override
    public List<OrderResponse> getOrdersByStatus(String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.findByStatus(orderStatus).stream()
                    .map(orderMapper::toResponse).toList();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut invalide: " + status);
        }
    }

    @Override
    public List<OrderResponse> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findByDateRange(startDate, endDate).stream()
                .map(orderMapper::toResponse).toList();
    }

    @Override
    public List<OrderResponse> getRecentOrders(int limit) {
        return orderRepository.findRecentCompletedOrders().stream()
                .limit(limit)
                .map(orderMapper::toResponse).toList();
    }

//    @Override
//    @Transactional(readOnly = true)
//    public List<OrderResponse> findCashierOrdersByShift(UUID cashierId, UUID shiftId) {
//        userRepository.findById(cashierId)
//                .orElseThrow(() -> new NotFoundException("Caissier non trouvé"));
//        return orderRepository.findCashierOrdersByShift(cashierId, shiftId).stream()
//                .map(orderMapper::toResponse).toList();
//    }

    @Override
    public BigDecimal getTotalSalesByStore(UUID storeId,
                                           LocalDateTime startDate,
                                           LocalDateTime endDate) {
        return getBigDecimal(storeId, startDate, endDate, orderRepository);
    }

    @Override
    public Integer getOrderCountByStore(UUID storeId,
                                        LocalDateTime startDate,
                                        LocalDateTime endDate) {
        return getInteger(storeId, startDate, endDate, orderRepository);
    }

    // =========================================================================
    // HELPERS STATIQUES — réutilisables depuis d'autres services
    // =========================================================================

    @NonNull
    public static BigDecimal getBigDecimal(UUID storeId, LocalDateTime startDate,
                                           LocalDateTime endDate,
                                           OrderRepository orderRepository) {
        if (startDate == null || endDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
            endDate   = LocalDateTime.now();
        }
        BigDecimal total = orderRepository
                .getTotalSalesByStoreAndDateRange(storeId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    @NonNull
    public static Integer getInteger(UUID storeId, LocalDateTime startDate,
                                     LocalDateTime endDate,
                                     OrderRepository orderRepository) {
        if (startDate == null || endDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
            endDate   = LocalDateTime.now();
        }
        Integer count = orderRepository
                .getOrderCountByStoreAndDateRange(storeId, startDate, endDate);
        return count != null ? count : 0;
    }
}