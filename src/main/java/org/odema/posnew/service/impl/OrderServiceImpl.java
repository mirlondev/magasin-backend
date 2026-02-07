package org.odema.posnew.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.OrderItemRequest;
import org.odema.posnew.dto.request.OrderRequest;
import org.odema.posnew.dto.response.OrderResponse;
import org.odema.posnew.entity.*;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.odema.posnew.entity.enums.UserRole;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.mapper.OrderMapper;
import org.odema.posnew.repository.*;
import org.odema.posnew.service.OrderService;
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
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request, UUID cashierId) {
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

        // Ensure all BigDecimal values have defaults
        BigDecimal taxRate = request.taxRate() != null ? request.taxRate() : BigDecimal.ZERO;
        BigDecimal discountAmount = request.discountAmount() != null ? request.discountAmount() : BigDecimal.ZERO;
        BigDecimal amountPaid = request.amountPaid() != null ? request.amountPaid() : BigDecimal.ZERO;
        Boolean isTaxable = request.isTaxable() != null ? request.isTaxable() : Boolean.FALSE;

        // Créer la commande
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .cashier(cashier)
                .store(store)
                .paymentMethod(request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.PENDING)
                .status(OrderStatus.PENDING)
                .notes(request.notes())
                .isTaxable(isTaxable)
                .taxRate(taxRate)
                .discountAmount(discountAmount)
                .amountPaid(amountPaid)
                // Initialize all BigDecimal fields to ZERO
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
            Integer availableStock = inventoryRepository.findTotalQuantityByProduct(itemRequest.productId());
            if (availableStock == null || availableStock < itemRequest.quantity()) {
                throw new BadRequestException("Stock insuffisant pour le produit: " + product.getName());
            }

            // Ensure discount has default value
            BigDecimal itemDiscountPercentage = itemRequest.discountPercentage() != null ?
                    itemRequest.discountPercentage() : BigDecimal.ZERO;

            // Créer l'article de commande
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(itemRequest.quantity())
                    .unitPrice(product.getPrice())
                    .discountPercentage(itemDiscountPercentage)
                    .discountAmount(BigDecimal.ZERO) // Initialize
                    .totalPrice(BigDecimal.ZERO) // Initialize
                    .finalPrice(BigDecimal.ZERO) // Initialize
                    .notes(itemRequest.notes())
                    .build();

            // Calculate prices for this item
            orderItem.calculatePrices();

            // Add item to order
            order.addItem(orderItem);
        }

        // Calculer les totaux
        order.calculateTotals();

        // Déterminer le statut de paiement
        if (order.getAmountPaid() != null && order.getTotalAmount() != null) {
            if (order.getAmountPaid().compareTo(order.getTotalAmount()) >= 0) {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setStatus(OrderStatus.COMPLETED);
                order.setCompletedAt(LocalDateTime.now());
            } else if (order.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
                order.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
                order.setStatus(OrderStatus.PROCESSING);
            }
        }

        // Sauvegarder la commande
        Order savedOrder = orderRepository.save(order);

        // Mettre à jour le stock
        updateInventoryForOrder(savedOrder);

        // Mettre à jour le client si applicable
        if (customer != null) {
            customer.addPurchase(savedOrder.getTotalAmount().doubleValue());
            customerRepository.save(customer);
        }

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        return orderMapper.toResponse(order);
    }

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
                Integer availableStock = inventoryRepository.findTotalQuantityByProduct(itemRequest.productId());
                if (availableStock == null || availableStock < itemRequest.quantity()) {
                    throw new BadRequestException("Stock insuffisant pour le produit: " + product.getName());
                }

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
    public OrderResponse processPayment(UUID orderId, BigDecimal amountPaid) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Impossible de traiter le paiement d'une commande annulée");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("La commande est déjà terminée");
        }

        order.setAmountPaid(amountPaid != null ? amountPaid : BigDecimal.ZERO);
        order.calculateTotals();

        // Vérifier si le paiement est suffisant
        if (order.getAmountPaid().compareTo(order.getTotalAmount()) >= 0) {
            order.markAsPaid();
        } else if (order.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            order.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        Order updatedOrder = orderRepository.save(order);
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

        if (order.getPaymentStatus() != PaymentStatus.PAID &&
                order.getPaymentStatus() != PaymentStatus.PARTIALLY_PAID) {
            throw new BadRequestException("Le paiement n'a pas été effectué");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toResponse(updatedOrder);
    }

    @Override
    public BigDecimal getTotalSalesByStore(UUID storeId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
            endDate = LocalDateTime.now();
        }

        BigDecimal total = orderRepository.getTotalSalesByStoreAndDateRange(
                storeId.toString(), startDate, endDate);

        return total != null ? total : BigDecimal.ZERO;
    }

    @Override
    public Integer getOrderCountByStore(UUID storeId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            startDate = LocalDateTime.now().minusDays(30);
            endDate = LocalDateTime.now();
        }

        Integer count = orderRepository.getOrderCountByStoreAndDateRange(
                storeId.toString(), startDate, endDate);

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
                    .map(orderMapper::toResponse)
                 ;
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