package org.odema.posnew.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.event.OrderCancelledEvent;
import org.odema.posnew.design.event.OrderCompletedEvent;
import org.odema.posnew.design.event.OrderCreatedEvent;
import org.odema.posnew.design.event.PaymentReceivedEvent;
import org.odema.posnew.design.factory.SaleStrategyFactory;
import org.odema.posnew.design.handler.PaymentHandler;
import org.odema.posnew.design.template.OrderServiceTemplate;
import org.odema.posnew.dto.request.OrderItemRequest;
import org.odema.posnew.dto.request.OrderRequest;
import org.odema.posnew.dto.request.PaymentRequest;
import org.odema.posnew.dto.response.OrderResponse;
import org.odema.posnew.entity.*;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.odema.posnew.entity.enums.ShiftStatus;
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
import java.util.ArrayList;
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

        // Build payment handler chain
        this.paymentHandlerChain = cashHandler;
        cashHandler.setNext(cardHandler);
        cardHandler.setNext(mobileHandler);
        mobileHandler.setNext(creditHandler);
    }

    public static void getDiscountPercentage(Order order, OrderItemRequest itemRequest, Product product) {

    }

    // ============================================================================
    // ORDER CREATION - NO PAYMENT HERE!
    // ============================================================================

    /**
     * ✅ Create order WITHOUT payment - payments added separately
     */
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, UUID cashierId) {
        log.info("Creating order - Type: {}, Cashier: {}", request.orderType(), cashierId);

        // 1. Validate request
        validateOrderRequest(request);

        // 2. Build order from request
        Order order = buildOrderFromRequest(request, cashierId);

        // 3. Calculate totals from items (NOT from payments!)
        order.calculateTotals();

        // 4. Validate totals > 0
        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Le montant total doit être supérieur à 0");
        }

        // 5. Save order
        Order savedOrder = orderRepository.save(order);

        // 6. Update inventory
        deductInventoryForOrder(savedOrder);

        // 7. Publish event
        eventPublisher.publishEvent(new OrderCreatedEvent(this, savedOrder));

        log.info("Order created successfully: {}, Total: {}",
                savedOrder.getOrderNumber(), savedOrder.getTotalAmount());

        return orderMapper.toResponse(savedOrder);
    }

    /**
     * ✅ Convenience method: Create order with initial payment
     */
    @Override
    @Transactional
    public OrderResponse createOrderWithPayment(OrderRequest orderRequest,
                                                PaymentRequest paymentRequest,
                                                UUID cashierId) throws UnauthorizedException {
        // 1. Create order first
        OrderResponse orderResponse = createOrder(orderRequest, cashierId);

        // 2. Add payment if amount > 0
        if (paymentRequest != null && paymentRequest.amount().compareTo(BigDecimal.ZERO) > 0) {
            return addPaymentToOrder(orderResponse.orderId(), paymentRequest, cashierId);
        }

        return orderResponse;
    }

    /**
     * Build order entity from request
     */
    private Order buildOrderFromRequest(OrderRequest request, UUID cashierId) {
        // Get cashier
        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new NotFoundException("Caissier non trouvé"));

        // Get store
        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new NotFoundException("Magasin non trouvé"));

        // Get customer (optional)
        Customer customer = null;
        if (request.customerId() != null) {
            customer = customerRepository.findById(request.customerId())
                    .orElse(null);
        }

        // Build order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .cashier(cashier)
                .store(store)
                .customer(customer)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .orderType(request.orderType())
                .discountAmount(request.discountAmount())
                .taxRate(request.taxRate())
                .isTaxable(request.isTaxable())
                .notes(request.notes())
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();

        // Add items
        for (OrderItemRequest itemRequest : request.items()) {
            OrderItem item = buildOrderItem(itemRequest, order);
            order.addItem(item);
        }

        return order;
    }

    /**
     * Build order item from request - prices calculated from product
     */
    private OrderItem buildOrderItem(OrderItemRequest request, Order order) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Produit non trouvé: " + request.productId()));

        // Check stock
        Inventory inventory = inventoryRepository
                .findByProduct_ProductIdAndStore_StoreId(product.getProductId(), order.getStore().getStoreId())
                .orElseThrow(() -> new BadRequestException("Produit non disponible dans ce magasin"));

        if (inventory.getQuantity() < request.quantity()) {
            throw new BadRequestException(String.format(
                    "Stock insuffisant pour %s. Disponible: %d, Demandé: %d",
                    product.getName(), inventory.getQuantity(), request.quantity()));
        }

        // Build item - prices calculated automatically
        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(request.quantity())
                .unitPrice(product.getPrice())
                .discountPercentage(request.discountPercentage())
                .notes(request.notes())
                .build();

        // Calculate prices
        item.calculatePrices();

        return item;
    }

    /**
     * Validate order request
     */
    private void validateOrderRequest(OrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("La commande doit contenir au moins un article");
        }
    }

    // ============================================================================
    // PAYMENT HANDLING - SEPARATE FROM ORDER CREATION
    // ============================================================================

    /**
     * ✅ Add payment to existing order
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse addPaymentToOrder(UUID orderId, PaymentRequest paymentRequest, UUID cashierId)
            throws UnauthorizedException {
        log.info("Adding payment {} to order {}", paymentRequest.amount(), orderId);

        // 1. Get order
        Order order = orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        // 2. Validate order can accept payment
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible d'ajouter un paiement à une commande annulée");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("La commande est déjà complétée");
        }

        // 3. Get cashier
        User cashier = userRepository.findById(cashierId)
                .orElseThrow(() -> new NotFoundException("Caissier non trouvé"));

        // 4. Get current shift (required for payment)
        ShiftReport shiftReport = shiftReportRepository
                .findOpenShiftByCashier(cashierId)
                .orElseThrow(() -> new BadRequestException("Aucune caisse ouverte pour ce caissier"));
       // findByCashier_UserIdAndStatus
        // 5. Create payment
        Payment payment = createPayment(paymentRequest, order, cashier, shiftReport);

        // 6. Add payment to order
        order.addPayment(payment);

        // 7. Update shift report
        if (payment.isActualPayment()) {
            shiftReport.addSale(payment.getAmount());
            shiftReportRepository.save(shiftReport);
        }

        // 8. Save order
        Order updatedOrder = orderRepository.save(order);

        // 9. Publish event
        eventPublisher.publishEvent(new PaymentReceivedEvent(this, updatedOrder, payment));

        log.info("Payment added successfully - New status: {}, Total paid: {}",
                updatedOrder.getPaymentStatus(), updatedOrder.getTotalPaid());

        return orderMapper.toResponse(updatedOrder);
    }

    /**
     * Create payment entity
     */
//    private Payment createPayment(PaymentRequest request, Order order, User cashier, ShiftReport shiftReport) {
//        PaymentStatus status = request.method() == PaymentMethod.CREDIT
//                ? PaymentStatus.CREDIT
//                : PaymentStatus.PAID;
//
//        Payment payment = Payment.builder()
//                .order(order)
//                .method(request.method())
//                .amount(request.amount())
//                .cashier(cashier)
//                .shiftReport(shiftReport)
//                .status(status)
//                .notes(request.notes())
//                .isActive(true)
//                .build();
//
//        return paymentRepository.save(payment);
//    }
    /**
     * Create payment entity
     */
    private Payment createPayment(PaymentRequest request, Order order, User cashier, ShiftReport shiftReport) {
        log.info("Creating payment - Method: {}, Amount: {}", request.method(), request.amount());

        // Determine status based on method
        PaymentStatus status;
        if (request.method() == PaymentMethod.CREDIT) {
            status = PaymentStatus.CREDIT;
            log.info("Payment is CREDIT type");
        } else {
            status = PaymentStatus.PAID;
            log.info("Payment is PAID type (method: {})", request.method());
        }

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

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment saved - ID: {}, Status: {}, Method: {}",
                savedPayment.getPaymentId(), savedPayment.getStatus(), savedPayment.getMethod());

        return savedPayment;
    }

    // ============================================================================
    // ORDER LIFECYCLE
    // ============================================================================

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse markAsCompleted(UUID orderId) {
        log.info("Marking order {} as completed", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible de terminer une commande annulée");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            return orderMapper.toResponse(order);
        }

        // Check payment
        if (order.getPaymentStatus() == PaymentStatus.UNPAID) {
            throw new BadRequestException("La commande n'a aucun paiement enregistré");
        }

        order.markAsCompleted();
        Order updatedOrder = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderCompletedEvent(this, updatedOrder));

        return orderMapper.toResponse(updatedOrder);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN')")
    public void cancelOrder(UUID orderId) {
        log.info("Cancelling order {}", orderId);

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

        restoreInventoryForOrder(order);
        eventPublisher.publishEvent(new OrderCancelledEvent(this, order));
    }

    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','CASHIER')")
    public OrderResponse updateOrder(UUID orderId, OrderRequest request) {
        log.info("Updating order {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible de modifier une commande " + order.getStatus());
        }

        // Update allowed fields
        if (request.customerId() != null) {
            Customer customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new NotFoundException("Client non trouvé"));
            order.setCustomer(customer);
        }

        if (request.notes() != null) order.setNotes(request.notes());
        if (request.discountAmount() != null) order.setDiscountAmount(request.discountAmount());
        if (request.taxRate() != null) order.setTaxRate(request.taxRate());
        if (request.isTaxable() != null) order.setIsTaxable(request.isTaxable());

        // Recalculate totals
        order.calculateTotals();

        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toResponse(updatedOrder);
    }

    // ============================================================================
    // INVENTORY MANAGEMENT
    // ============================================================================

    private void deductInventoryForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryRepository.findByProduct_ProductIdAndStore_StoreId(
                    item.getProduct().getProductId(),
                    order.getStore().getStoreId()
            ).ifPresent(inventory -> {
                inventory.decreaseQuantity(item.getQuantity());
                inventoryRepository.save(inventory);
                log.debug("Stock deducted: {} x{} for product {}",
                        item.getQuantity(), item.getProduct().getName());
            });
        }
    }

    private void restoreInventoryForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryRepository.findByProduct_ProductIdAndStore_StoreId(
                    item.getProduct().getProductId(),
                    order.getStore().getStoreId()
            ).ifPresent(inventory -> {
                inventory.increaseQuantity(item.getQuantity());
                inventoryRepository.save(inventory);
                log.debug("Stock restored: {} x{} for product {}",
                        item.getQuantity(), item.getProduct().getName());
            });
        }
    }

    // ============================================================================
    // QUERIES
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
            case ADMIN -> orderRepository.findAll(pageable).map(orderMapper::toResponse);
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
    public List<OrderResponse> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
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

    @Override
    public BigDecimal getTotalSalesByStore(UUID storeId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
            endDate = LocalDateTime.now();
        }

        BigDecimal total = orderRepository.getTotalSalesByStoreAndDateRange(storeId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public Integer getOrderCountByStore(UUID storeId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
            endDate = LocalDateTime.now();
        }

        Integer count = orderRepository.getOrderCountByStoreAndDateRange(storeId, startDate, endDate);
        return count != null ? count : 0;
    }

    // ============================================================================
    // HELPERS
    // ============================================================================

    protected String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }
}