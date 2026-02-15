package org.odema.posnew.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.OrderItemRequest;
import org.odema.posnew.dto.request.OrderRequest;
import org.odema.posnew.dto.response.OrderResponse;
import org.odema.posnew.dto.request.PaymentRequest;
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
import org.odema.posnew.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceOldImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderMapper orderMapper;
    private final PaymentService paymentService;
    private final ShiftReportRepository shiftReportRepository;
    private final PaymentRepository paymentRepository;


    @Override
    public OrderResponse getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(UUID orderId, OrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // Vérifier que la commande peut être modifiée
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible de modifier une commande " + order.getStatus());
        }

        // Mettre à jour les informations de base
        if (request.customerId() != null) {
            Customer customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new NotFoundException("Client non trouvé"));
            order.setCustomer(customer);
        }

        if (request.notes() != null) {
            order.setNotes(request.notes());
        }

        if (request.paymentMethod() != null) {
            order.setPaymentMethod(request.paymentMethod());
        }

        if (request.isTaxable() != null) {
            order.setIsTaxable(request.isTaxable());
        }

        if (request.taxRate() != null) {
            order.setTaxRate(request.taxRate());
        }

        if (request.discountAmount() != null) {
            order.setDiscountAmount(request.discountAmount());
        }

        // Mettre à jour les articles si spécifiés
        if (request.items() != null && !request.items().isEmpty()) {
            // Supprimer les anciens articles
            order.getItems().clear();

            // Ajouter les nouveaux articles
            for (OrderItemRequest itemRequest : request.items()) {
                Product product = productRepository.findById(itemRequest.productId())
                        .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

                // Vérifier le stock
                availableStock(order, itemRequest, product);
            }
        }

        // Recalculer les totaux
        order.calculateTotals();

        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toResponse(updatedOrder);
    }

    @Override
    @Transactional
    public void cancelOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("La commande est déjà annulée");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Impossible d'annuler une commande terminée");
        }

        order.cancelOrder();
        orderRepository.save(order);

        // Restaurer le stock
        restoreInventoryForOrder(order);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(orderMapper::toResponse)
                .toList();
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
            throw new BadRequestException("Statut de commande invalide: " + status);
        }
    }

    @Override
    public List<OrderResponse> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findByDateRange(startDate, endDate).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, UUID cashierId) throws UnauthorizedException {
        // Récupérer le caissier
        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new NotFoundException("Caissier non trouvé"));

        // Récupérer le store
        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        // Récupérer le client si spécifié
        Customer customer = null;
        if (request.customerId() != null) {
            customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new NotFoundException("Client non trouvé"));
        }

        // Générer un numéro de commande unique
        String orderNumber = generateOrderNumber();

        // Valeurs par défaut
        BigDecimal taxRate = request.taxRate() != null ? request.taxRate() : BigDecimal.ZERO;
        BigDecimal discountAmount = request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO;
        Boolean isTaxable = request.isTaxable() != null ? request.isTaxable() : Boolean.FALSE;

        // Créer la commande avec le nouveau système
        PaymentMethod paymentMethod1 = request.paymentMethod() != null ?
                request.paymentMethod() : PaymentMethod.CASH;

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .cashier(cashier)
                .store(store)
                .paymentMethod(paymentMethod1)
                .paymentStatus(PaymentStatus.UNPAID) // Par défaut non payé
                .status(OrderStatus.PENDING)
                .notes(request.notes())
                .isTaxable(isTaxable)
                .taxRate(taxRate)
                .discountAmount(discountAmount)
                .amountPaid(BigDecimal.ZERO) // Initialisé à 0
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .changeAmount(BigDecimal.ZERO)
                .build();

        // Ajouter les articles
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé: " + itemRequest.productId()));

            // Vérifier le stock
            availableStock(order, itemRequest, product);
        }

        // Calculer les totaux
        order.calculateTotals();

        // Sauvegarder la commande AVANT d'ajouter le paiement
        Order savedOrder = orderRepository.save(order);

        // Si un paiement initial est spécifié, le créer directement
        if (request.amountPaid() != null && request.amountPaid().compareTo(BigDecimal.ZERO) > 0) {
            PaymentMethod paymentMethod = paymentMethod1;

            // Récupérer le shift report ouvert
            ShiftReport shift = shiftReportRepository.findOpenShiftByCashier(cashierId)
                    .orElse(null);

            // Créer le paiement directement (sans passer par PaymentService pour éviter la validation)
            Payment payment = Payment.builder()
                    .order(savedOrder)
                    .method(paymentMethod)
                    .amount(request.amountPaid())
                    .cashier(cashier)
                    .shiftReport(shift)
                    .status(paymentMethod == PaymentMethod.CREDIT ?
                            PaymentStatus.CREDIT : PaymentStatus.PAID)
                    .notes("Paiement initial lors de la création de la commande")
                    .isActive(true)
                    .build();

            // Sauvegarder le paiement
            Payment savedPayment = paymentRepository.save(payment);

            // Ajouter le paiement à la commande
            savedOrder.addPayment(savedPayment);

            // Mettre à jour le shift report (sauf pour crédit)
            if (shift != null && paymentMethod != PaymentMethod.CREDIT) {
                shift.addSale(request.amountPaid());
                shiftReportRepository.save(shift);
            }

            // Si la commande est maintenant complètement payée, la marquer comme terminée
            if (savedOrder.getPaymentStatus() == PaymentStatus.PAID &&
                    savedOrder.getStatus() == OrderStatus.PENDING) {
                savedOrder.setStatus(OrderStatus.COMPLETED);
                savedOrder.setCompletedAt(LocalDateTime.now());
            }

            // Sauvegarder la commande mise à jour
            savedOrder = orderRepository.save(savedOrder);
        }

        // Mettre à jour le stock
        updateInventoryForOrder(savedOrder);

        // Mettre à jour le client si applicable
        if (customer != null) {
            customer.addPurchase(savedOrder.getTotalAmount().doubleValue());
            customerRepository.save(customer);
        }

        return orderMapper.toResponse(savedOrder);
    }

    private void availableStock(Order order, OrderItemRequest itemRequest, Product product) {
        Integer availableStock = inventoryRepository.findTotalQuantityByProduct(itemRequest.productId());
        if (availableStock == null || availableStock < itemRequest.quantity()) {
            throw new BadRequestException("Stock insuffisant pour le produit: " + product.getName());
        }

        getDiscountPercentage(order, itemRequest, product);
    }

    public static void getDiscountPercentage(Order order, OrderItemRequest itemRequest, Product product) {
        BigDecimal itemDiscountPercentage = itemRequest.discountPercentage() != null ?
                itemRequest.discountPercentage() : BigDecimal.ZERO;

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(itemRequest.quantity())
                .unitPrice(product.getPrice())
                .discountPercentage(itemDiscountPercentage)
                .discountAmount(BigDecimal.ZERO)
                .totalPrice(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .notes(itemRequest.notes())
                .build();

        orderItem.calculatePrices();
        order.addItem(orderItem);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    @Override
    public OrderResponse addPaymentToOrder(UUID orderId, PaymentRequest paymentRequest) throws UnauthorizedException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible d'ajouter un paiement à une commande annulée");
        }

        // Traiter le paiement via le PaymentService
        paymentService.processPayment(orderId, paymentRequest, order.getCashier().getUserId());

        // Recharger la commande mise à jour
        Order updatedOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        return orderMapper.toResponse(updatedOrder);
    }

    @Override
    @Transactional
    public OrderResponse markAsCompleted(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible de terminer une commande annulée");
        }

        // Vérifier si la commande est payée ou partiellement payée
        if (order.getPaymentStatus() != PaymentStatus.PAID &&
                order.getPaymentStatus() != PaymentStatus.PARTIALLY_PAID &&
                order.getPaymentStatus() != PaymentStatus.CREDIT) {
            throw new BadRequestException("La commande n'a pas de paiement enregistré");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toResponse(updatedOrder);
    }

    @Override
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        return orderMapper.toResponse(order);
    }

    @Override
    public BigDecimal getTotalSalesByStore(UUID storeId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
            endDate = LocalDateTime.now();
        }

        BigDecimal total = orderRepository.getTotalSalesByStoreAndDateRange(
                storeId, startDate, endDate);

        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findCashierOrdersByShift(UUID cashierId, UUID shiftId) {
        // 1. Vérifier le caissier
        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new NotFoundException("Caissier non trouvé"));

        // 2. Récupérer les commandes du caissier pour ce shift
        List<Order> orders = orderRepository
                .findCashierOrdersByShift(cashierId, shiftId);

        // 3. Mapper vers OrderResponse
        return orders.stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public Integer getOrderCountByStore(UUID storeId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
            endDate = LocalDateTime.now();
        }

        Integer count = orderRepository.getOrderCountByStoreAndDateRange(
                storeId, startDate, endDate);

        return count != null ? count : 0;
    }

    @Override
    public List<OrderResponse> getRecentOrders(int limit) {
        return orderRepository.findRecentCompletedOrders().stream()
                .limit(limit)
                .map(orderMapper::toResponse)
                .toList();
    }

    @Override
    public Page<OrderResponse> getOrders(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

        // ADMIN → tout
        if (user.getUserRole() == UserRole.ADMIN) {
            return orderRepository.findAll(pageable)
                    .map(orderMapper::toResponse);
        }

        // STORE ADMIN → commandes du magasin
        if (user.getUserRole() == UserRole.STORE_ADMIN) {
            return orderRepository
                    .findByStore_StoreId(user.getAssignedStore().getStoreId(), pageable)
                    .map(orderMapper::toResponse);
        }

        // CASHIER → ses commandes uniquement
        if (user.getUserRole() == UserRole.CASHIER) {
            return orderRepository
                    .findByCashier_UserId(user.getUserId(), pageable)
                    .map(orderMapper::toResponse);
        }

        throw new BadRequestException("Rôle non autorisé");
    }

    private String generateOrderNumber() {
        String prefix = "ORD";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf((int) (Math.random() * 1000));

        String substring = timestamp.substring(timestamp.length() - 6);
        String orderNumber = prefix + substring + random;

        // Vérifier l'unicité
        while (orderRepository.existsByOrderNumber(orderNumber)) {
            random = String.valueOf((int) (Math.random() * 1000));
            orderNumber = prefix + substring + random;
        }

        return orderNumber;
    }

    private void updateInventoryForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            // Pour chaque article, trouver l'inventaire dans le store de la commande
            inventoryRepository.findByProduct_ProductIdAndStore_StoreId(
                            item.getProduct().getProductId(),
                            order.getStore().getStoreId())
                    .ifPresent(inventory -> {
                        inventory.decreaseQuantity(item.getQuantity());
                        inventoryRepository.save(inventory);
                    });
        }
    }

    private void restoreInventoryForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryRepository.findByProduct_ProductIdAndStore_StoreId(
                            item.getProduct().getProductId(),
                            order.getStore().getStoreId())
                    .ifPresent(inventory -> {
                        inventory.increaseQuantity(item.getQuantity());
                        inventoryRepository.save(inventory);
                    });
        }
    }
}
