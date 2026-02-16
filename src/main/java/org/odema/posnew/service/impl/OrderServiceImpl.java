package org.odema.posnew.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.event.OrderCancelledEvent;
import org.odema.posnew.design.event.OrderCompletedEvent;
import org.odema.posnew.design.event.OrderCreatedEvent;
import org.odema.posnew.design.event.PaymentReceivedEvent;
import org.odema.posnew.design.factory.SaleStrategyFactory;
import org.odema.posnew.design.handler.PaymentHandler;
import org.odema.posnew.design.strategy.SaleStrategy;
import org.odema.posnew.design.template.OrderServiceTemplate;
import org.odema.posnew.dto.request.OrderItemRequest;
import org.odema.posnew.dto.request.OrderRequest;
import org.odema.posnew.dto.request.PaymentRequest;
import org.odema.posnew.dto.response.OrderResponse;
import org.odema.posnew.entity.*;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.odema.posnew.entity.enums.UserRole;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.exception.UnauthorizedException;
import org.odema.posnew.mapper.OrderMapper;
import org.odema.posnew.repository.*;
import org.odema.posnew.service.OrderService;
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
    private final PaymentHandler paymentHandlerChain;
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

        // ✅ Construire la chaîne de responsabilité COMPLÈTE
        this.paymentHandlerChain = cashHandler;
        cashHandler.setNext(cardHandler);
        cardHandler.setNext(mobileHandler);
        mobileHandler.setNext(creditHandler);
    }


    // ============================================================================
    // MÉTHODES TEMPLATE METHOD - Hooks et surcharges
    // ============================================================================

    /**
     * ✅ Surcharge UNIQUE de createOrder avec publication d'événement
     */
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, UUID cashierId) {
        log.info("Création commande - Type: {}, Cashier: {}",
                request.orderType(), cashierId);

        // ✅ Appeler le Template Method du parent
        OrderResponse response = super.createOrder(request, cashierId);

        // ✅ Publier événement métier
        Order order = orderRepository.findById(response.orderId())
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        eventPublisher.publishEvent(new OrderCreatedEvent(this, order));

        log.info("Commande créée avec succès: {}", order.getOrderNumber());

        return response;
    }

    /**
     * ✅ Hook pour ajouter paiement initial (appelé par le Template Method)
     */
    @Override
    protected Order afterOrderCreation(Order order, OrderRequest request,
                                       SaleStrategy strategy) {
        // ✅ Si paiement initial fourni, le traiter
        if (request.amountPaid() != null &&
                request.amountPaid().compareTo(BigDecimal.ZERO) > 0) {

            log.info("Ajout paiement initial: {} pour commande {}",
                    request.amountPaid(), order.getOrderNumber());

            PaymentMethod method = request.paymentMethod() != null
                    ? request.paymentMethod() : PaymentMethod.CASH;

            PaymentRequest paymentRequest = new PaymentRequest(
                    method,
                    request.amountPaid(),
                    "Paiement initial lors de la création"
            );

            try {
                // ✅ Utiliser la chaîne de handlers pour traiter le paiement
                Payment payment = paymentHandlerChain.handle(paymentRequest, order);
                order.addPayment(payment);

                log.info("Paiement initial ajouté: {} {}",
                        payment.getAmount(), payment.getMethod());
            } catch (Exception e) {
                log.error("Erreur ajout paiement initial", e);
                // Ne pas bloquer la création de commande si paiement échoue
            }
        }

        return order;
    }

    // ============================================================================
    // MÉTHODES DE PAIEMENT
    // ============================================================================

    /**
     * ✅ Ajouter un paiement à une commande existante
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse addPaymentToOrder(UUID orderId, PaymentRequest paymentRequest)
            throws UnauthorizedException {
        log.info("Ajout paiement {} à commande {}",
                paymentRequest.amount(), orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // ✅ Vérifications métier
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible d'ajouter un paiement à une commande annulée");
        }

        // ✅ Utiliser la chaîne de handlers
        Payment payment = paymentHandlerChain.handle(paymentRequest, order);
        order.addPayment(payment);

        // ✅ Sauvegarder
        Order updatedOrder = orderRepository.save(order);

        // ✅ Publier événement paiement
        eventPublisher.publishEvent(
                new PaymentReceivedEvent(this, updatedOrder, payment)
        );

        log.info("Paiement ajouté avec succès - Nouveau statut: {}",
                updatedOrder.getPaymentStatus());

        return orderMapper.toResponse(updatedOrder);
    }

    // ============================================================================
    // MÉTHODES DE CYCLE DE VIE
    // ============================================================================

    /**
     * ✅ Marquer commande comme complétée
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse markAsCompleted(UUID orderId) {
        log.info("Marquage commande {} comme complétée", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // ✅ Validations
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible de terminer une commande annulée");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.warn("Commande {} déjà complétée", orderId);
            return orderMapper.toResponse(order);
        }

        // ✅ Vérifier paiement
        if (order.getPaymentStatus() == PaymentStatus.UNPAID) {
            throw new BadRequestException("La commande n'a aucun paiement enregistré");
        }

        // ✅ Marquer comme complétée
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);

        // ✅ Publier événement
        eventPublisher.publishEvent(new OrderCompletedEvent(this, updatedOrder));

        log.info("Commande {} complétée avec succès", order.getOrderNumber());

        return orderMapper.toResponse(updatedOrder);
    }

    /**
     * ✅ Annuler une commande
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN')")
    public void cancelOrder(UUID orderId) {
        log.info("Annulation commande {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // ✅ Validations
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("La commande est déjà annulée");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Impossible d'annuler une commande terminée");
        }

        // ✅ Annuler
        order.cancelOrder();
        orderRepository.save(order);

        // ✅ Restaurer le stock
        restoreInventoryForOrder(order);

        // ✅ Publier événement
        eventPublisher.publishEvent(new OrderCancelledEvent(this, order));

        log.info("Commande {} annulée - Stock restauré", order.getOrderNumber());
    }

    /**
     * ✅ Mettre à jour une commande
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse updateOrder(UUID orderId, OrderRequest request) {
        log.info("Mise à jour commande {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // ✅ Vérifier que la commande peut être modifiée
        if (order.getStatus() == OrderStatus.COMPLETED ||
                order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException(
                    "Impossible de modifier une commande " + order.getStatus()
            );
        }

        // ✅ Mettre à jour les champs autorisés
        updateOrderFields(order, request);

        // ✅ Recalculer les totaux
        order.calculateTotals();

        Order updatedOrder = orderRepository.save(order);

        log.info("Commande {} mise à jour", order.getOrderNumber());

        return orderMapper.toResponse(updatedOrder);
    }

    /**
     * ✅ Méthode helper pour mise à jour des champs
     */
    private void updateOrderFields(Order order, OrderRequest request) {
        if (request.customerId() != null) {
            Customer customer = customerRepository.findById(
                    request.customerId()
            ).orElseThrow(() -> new NotFoundException("Client non trouvé"));
            order.setCustomer(customer);
        }

        if (request.notes() != null) {
            order.setNotes(request.notes());
        }

        if (request.discountAmount() != null) {
            order.setDiscountAmount(request.discountAmount());
        }

        if (request.taxRate() != null) {
            order.setTaxRate(request.taxRate());
        }

        if (request.isTaxable() != null) {
            order.setIsTaxable(request.isTaxable());
        }

        // ⚠️ Note: Modification des items non recommandée après création
        // Si nécessaire, implémenter avec restauration/déduction de stock
    }

    // ============================================================================
    // MÉTHODES DE CONSULTATION
    // ============================================================================

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));
        return orderMapper.toResponse(order);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));
        return orderMapper.toResponse(order);
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
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public List<OrderResponse> getOrdersByCustomer(UUID customerId) {
        return orderRepository.findByCustomer_CustomerId(customerId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public List<OrderResponse> getOrdersByCashier(UUID cashierId) {
        return orderRepository.findByCashier_UserId(cashierId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public List<OrderResponse> getOrdersByStatus(String status) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.findByStatus(orderStatus).stream()
                    .map(orderMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut invalide: " + status);
        }
    }

    @Override
    public List<OrderResponse> getOrdersByDateRange(LocalDateTime startDate,
                                                    LocalDateTime endDate) {
        return orderRepository.findByDateRange(startDate, endDate).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public List<OrderResponse> getRecentOrders(int limit) {
        return orderRepository.findRecentCompletedOrders().stream()
                .limit(limit)
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findCashierOrdersByShift(UUID cashierId, UUID shiftId) {
        userRepository.findById(cashierId)
                .orElseThrow(() -> new NotFoundException("Caissier non trouvé"));

        List<Order> orders = orderRepository.findCashierOrdersByShift(cashierId, shiftId);

        return orders.stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    // ============================================================================
    // MÉTHODES STATISTIQUES
    // ============================================================================

    @Override
    public BigDecimal getTotalSalesByStore(UUID storeId,
                                           LocalDateTime startDate,
                                           LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
            endDate = LocalDateTime.now();
        }

        BigDecimal total = orderRepository.getTotalSalesByStoreAndDateRange(
                storeId, startDate, endDate);

        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public Integer getOrderCountByStore(UUID storeId,
                                        LocalDateTime startDate,
                                        LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
            endDate = LocalDateTime.now();
        }

        Integer count = orderRepository.getOrderCountByStoreAndDateRange(
                storeId, startDate, endDate);

        return count != null ? count : 0;
    }

    // ============================================================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ============================================================================

    /**
     * ✅ Restaurer l'inventaire lors de l'annulation
     */
    private void restoreInventoryForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryRepository.findByProduct_ProductIdAndStore_StoreId(
                    item.getProduct().getProductId(),
                    order.getStore().getStoreId()
            ).ifPresent(inventory -> {
                inventory.increaseQuantity(item.getQuantity());
                inventoryRepository.save(inventory);

                log.debug("Stock restauré: {} x{} pour produit {}",
                        item.getQuantity(),
                        item.getProduct().getName(),
                        inventory.getInventoryId());
            });
        }
    }

    public static void getDiscountPercentage(Order order, OrderItemRequest itemRequest, Product product) {
        if (itemRequest.discountAmount() != null && itemRequest.discountAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountPercentage = itemRequest.discountAmount().divide(product.getPrice(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            log.info("Calcul pour produit {}: Montant de réduction {} correspond à une réduction de {}%",
                    product.getName(), itemRequest.discountAmount(), discountPercentage);
        } else {
            log.info("Aucune réduction appliquée pour produit {}", product.getName());
        }
    }
}