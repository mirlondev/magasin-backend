package org.odema.posnew.design.template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.odema.posnew.design.factory.SaleStrategyFactory;
import org.odema.posnew.design.strategy.SaleStrategy;
import org.odema.posnew.design.strategy.ValidationResult;
import org.odema.posnew.dto.request.OrderRequest;
import org.odema.posnew.dto.response.OrderResponse;
import org.odema.posnew.entity.*;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.odema.posnew.exception.BadRequestException;

import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.mapper.OrderMapper;
import org.odema.posnew.repository.*;
import org.odema.posnew.service.impl.OrderServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public abstract class OrderServiceTemplate {

    protected final OrderRepository orderRepository;
    protected final OrderItemRepository orderItemRepository;
    protected final CustomerRepository customerRepository;
    protected final UserRepository userRepository;
    protected final StoreRepository storeRepository;
    protected final ProductRepository productRepository;
    protected final InventoryRepository inventoryRepository;
    protected final OrderMapper orderMapper;
    protected final SaleStrategyFactory strategyFactory;

    /**
     * TEMPLATE METHOD - Squelette de création de commande
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request, UUID cashierId) {
        log.info("Création commande - Type: {}, Cashier: {}",
                request.orderType(), cashierId);

        // 1. Récupérer la stratégie
        SaleStrategy strategy = strategyFactory.getStrategy(request.orderType());

        // 2. Charger les entités nécessaires
        User cashier = loadCashier(cashierId);
        Store store = loadStore(request.storeId());
        Customer customer = loadCustomer(request.customerId());

        // 3. Créer la commande de base
        Order order = buildBaseOrder(request, cashier, store, customer);

        // 4. Ajouter les articles
        addOrderItems(order, request);

        // 5. Calculer les totaux
        order.calculateTotals();

        // 6. Valider selon stratégie
        ValidationResult validation = strategy.validate(request, order);
        if (!validation.isValid()) {
            throw new BadRequestException(
                    "Validation échouée: " + String.join(", ", validation.getErrors())
            );
        }

        // 7. Préparer selon stratégie
        strategy.prepareOrder(order, request);

        // 8. Sauvegarder
        Order savedOrder = orderRepository.save(order);

        // 9. Hook: Traitement post-création (paiement initial, etc.)
        savedOrder = afterOrderCreation(savedOrder, request, strategy);

        // 10. Finaliser selon stratégie
        strategy.finalizeOrder(savedOrder);

        // 11. Sauvegarder état final
        savedOrder = orderRepository.save(savedOrder);

        // 12. Mettre à jour inventaire
        updateInventoryForOrder(savedOrder);

        // 13. Mettre à jour client
        updateCustomerStatistics(savedOrder);

        log.info("Commande créée avec succès: {}", savedOrder.getOrderNumber());

        return orderMapper.toResponse(savedOrder);
    }

    /**
     * Hook method - À surcharger si besoin de traitement spécial
     */
    protected Order afterOrderCreation(Order order, OrderRequest request,
                                       SaleStrategy strategy) {
        // Comportement par défaut: rien
        // Les sous-classes peuvent surcharger pour ajouter paiement initial, etc.
        return order;
    }

    /**
     * Construire la commande de base
     */
    private Order buildBaseOrder(OrderRequest request, User cashier,
                                 Store store, Customer customer) {
        String orderNumber = generateOrderNumber();

        BigDecimal taxRate = request.taxRate() != null
                ? request.taxRate() : BigDecimal.ZERO;
        BigDecimal discountAmount = request.discountAmount() != null
                ? request.discountAmount() : BigDecimal.ZERO;
        Boolean isTaxable = request.isTaxable() != null
                ? request.isTaxable() : Boolean.FALSE;

        return Order.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .cashier(cashier)
                .store(store)
                .paymentStatus(PaymentStatus.UNPAID)
                .status(OrderStatus.PENDING)
                .notes(request.notes())
                .isTaxable(isTaxable)
                .taxRate(taxRate)
                .discountAmount(discountAmount)
                .amountPaid(BigDecimal.ZERO)
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .changeAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();
    }

    /**
     * Ajouter les articles à la commande
     */
    private void addOrderItems(Order order, OrderRequest request) {
        for (var itemRequest : request.items()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new NotFoundException(
                            "Produit non trouvé: " + itemRequest.productId()
                    ));

            // Vérifier stock
            validateStock(product, itemRequest.quantity());

            OrderServiceImpl.getDiscountPercentage(order, itemRequest, product);
        }
    }

    /**
     * Valider le stock disponible
     */
    private void validateStock(Product product, Integer requestedQty) {
        Integer availableStock = inventoryRepository
                .findTotalQuantityByProduct(product.getProductId());

        if (availableStock == null || availableStock < requestedQty) {
            throw new BadRequestException(
                    "Stock insuffisant pour: " + product.getName() +
                            " (disponible: " + (availableStock != null ? availableStock : 0) +
                            ", demandé: " + requestedQty + ")"
            );
        }
    }

    /**
     * Mettre à jour l'inventaire
     */
    private void updateInventoryForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryRepository.findByProduct_ProductIdAndStore_StoreId(
                    item.getProduct().getProductId(),
                    order.getStore().getStoreId()
            ).ifPresent(inventory -> {
                inventory.decreaseQuantity(item.getQuantity());
                inventoryRepository.save(inventory);
            });
        }
    }

    /**
     * Mettre à jour les statistiques client
     */
    private void updateCustomerStatistics(Order order) {
        if (order.getCustomer() != null) {
            Customer customer = order.getCustomer();
            customer.addPurchase(order.getTotalAmount().doubleValue());
            customerRepository.save(customer);
        }
    }

    // Méthodes de chargement
    private User loadCashier(UUID cashierId) {
        return userRepository.findById(cashierId)
                .orElseThrow(() -> new NotFoundException("Caissier non trouvé"));
    }

        private Store loadStore(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));
    }

    private Customer loadCustomer(UUID customerId) {
        if (customerId == null) return null;
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé"));
    }

    /**
     * Générer numéro de commande unique
     */
    protected String generateOrderNumber() {
        return getOrderPrefix(orderRepository);
    }

    @NonNull
    public static String getOrderPrefix(OrderRepository orderRepository) {
        String prefix = "ORD";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf((int) (Math.random() * 1000));

        String substring = timestamp.substring(timestamp.length() - 6);
        String orderNumber = prefix + substring + random;

        while (orderRepository.existsByOrderNumber(orderNumber)) {
            random = String.valueOf((int) (Math.random() * 1000));
            orderNumber = prefix + substring + random;
        }

        return orderNumber;
    }
}