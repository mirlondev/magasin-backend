package org.odema.posnew.design.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.application.dto.request.OrderItemRequest;
import org.odema.posnew.application.dto.request.OrderRequest;
import org.odema.posnew.application.dto.response.OrderResponse;
import org.odema.posnew.application.mapper.OrderMapper;
import org.odema.posnew.design.factory.SaleStrategyFactory;
import org.odema.posnew.domain.model.*;
import org.odema.posnew.domain.model.enums.OrderStatus;
import org.odema.posnew.domain.model.enums.PaymentStatus;
import org.odema.posnew.domain.repository.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    // =========================================================================
    // TEMPLATE METHOD — squelette commun de création de commande
    // =========================================================================

    /**
     * Crée une commande sans paiement.
     * Les sous-classes peuvent surcharger {@link #afterOrderSaved} pour
     * ajouter un comportement post-sauvegarde (événements, notifications…).
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request, UUID cashierId) {
        log.info("Création commande - Type: {}, Caissier: {}", request.orderType(), cashierId);

        // 1. Validation de base
        validateOrderRequest(request);

        // 2. Charger les entités
        User    cashier  = loadCashier(cashierId);
        Store   store    = loadStore(request.storeId());
        Customer customer = loadCustomer(request.customerId());

        // 3. Construire la commande
        Order order = buildBaseOrder(request, cashier, store, customer);

        // 4. Ajouter les articles (stock vérifié + prix snapshottés)
        addOrderItems(order, request);

        // 5. Valider que le total est cohérent
        //    getTotalAmount() est @Transient — calculé à la volée depuis les items
        if (order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Le montant total doit être supérieur à 0");
        }

        // 6. Sauvegarder
        Order savedOrder = orderRepository.save(order);

        // 7. Déduire le stock
        updateInventoryForOrder(savedOrder);

        // 8. Mettre à jour les stats client
       // updateCustomerStatistics(savedOrder);

        // 9. Hook post-sauvegarde (events, etc.) — surchargeable
        afterOrderSaved(savedOrder, request);

        log.info("Commande créée: {}, Total: {}", savedOrder.getOrderNumber(), savedOrder.getTotalAmount());
        return orderMapper.toResponse(savedOrder);
    }

    // =========================================================================
    // HOOK — À surcharger dans les sous-classes si besoin
    // =========================================================================

    /**
     * Appelé après la première sauvegarde de la commande.
     * Comportement par défaut : rien.
     * Exemple d'override : publier un OrderCreatedEvent, envoyer une notification…
     */
    protected void afterOrderSaved(Order savedOrder, OrderRequest request) {
        // no-op par défaut
    }

    // =========================================================================
    // CONSTRUCTION DE LA COMMANDE
    // =========================================================================

    private Order buildBaseOrder(OrderRequest request, User cashier, Store store, Customer customer) {
        return Order.builder()
                .orderNumber(generateOrderNumber())
                .cashier(cashier)
                .store(store)
                .customer(customer)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .orderType(request.orderType())
                // Remise globale — les deux champs existent bien dans Order
                .globalDiscountPercentage(
                        request.globalDiscountPercentage() != null
                                ? request.globalDiscountPercentage()
                                : BigDecimal.ZERO)
                .globalDiscountAmount(
                        request.discountAmount() != null
                                ? request.discountAmount()
                                : BigDecimal.ZERO)
                .notes(request.notes())
                .items(new ArrayList<>())
                .payments(new ArrayList<>())
                .build();
    }

    // =========================================================================
    // ARTICLES
    // =========================================================================

    private void addOrderItems(Order order, OrderRequest request) {
        for (OrderItemRequest itemRequest : request.items()) {

            // Charger le produit
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new NotFoundException(
                            "Produit non trouvé: " + itemRequest.productId()));

            // Vérifier le stock dans CE magasin via Inventory
            Inventory inventory = inventoryRepository
                    .findByProduct_ProductIdAndStore_StoreId(
                            product.getProductId(),
                            order.getStore().getStoreId())
                    .orElseThrow(() -> new BadRequestException(
                            "Produit non disponible dans ce magasin: " + product.getName()));

            if (inventory.getQuantity() < itemRequest.quantity()) {
                throw new BadRequestException(String.format(
                        "Stock insuffisant pour %s. Disponible: %d, Demandé: %d",
                        product.getName(), inventory.getQuantity(), itemRequest.quantity()));
            }

            // Récupérer le prix actif du magasin (StoreProductPrice)
            StoreProductPrice storePrice = product.getPriceForStore(
                    order.getStore().getStoreId());

            if (storePrice == null) {
                throw new BadRequestException(
                        "Aucun prix configuré pour ce produit dans ce magasin: " + product.getName());
            }

            // Construire l'OrderItem via la factory method du modèle
            //  → snapshote basePrice, taxRate, discountPercentage/Amount
            OrderItem item = OrderItem.fromStorePrice(product, storePrice, itemRequest.quantity());
            item.setOrder(order);

            // Remise spécifique à l'article si précisée dans la requête
            if (itemRequest.discountPercentage() != null) {
                item.setDiscountPercentage(itemRequest.discountPercentage());
            }
            if (itemRequest.notes() != null) {
                item.setNotes(itemRequest.notes());
            }

            // Déclencher le recalcul (@PrePersist/calculate())
            item.calculate();

            order.addItem(item);
        }
    }

    // =========================================================================
    // INVENTAIRE
    // =========================================================================

    protected void updateInventoryForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryRepository.findByProduct_ProductIdAndStore_StoreId(
                    item.getProduct().getProductId(),
                    order.getStore().getStoreId()
            ).ifPresent(inv -> {
                // Déduire en unités de BASE, pas en unités de commande
                int baseQty = item.getBaseQuantity() != null
                        ? item.getBaseQuantity().intValue()
                        : item.getQuantity();
                inv.decreaseQuantity(baseQty);
                inventoryRepository.save(inv);
            });
        }
    }

    protected void restoreInventoryForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            inventoryRepository.findByProduct_ProductIdAndStore_StoreId(
                    item.getProduct().getProductId(),
                    order.getStore().getStoreId()
            ).ifPresent(inv -> {
                inv.increaseQuantity(item.getQuantity());
                inventoryRepository.save(inv);
                log.debug("Stock restauré: {} x {} (produit: {})",
                        item.getQuantity(), item.getProduct().getName(),
                        item.getProduct().getProductId());
            });
        }
    }

    // =========================================================================
    // STATISTIQUES CLIENT
    // =========================================================================

   /* private void updateCustomerStatistics(Order order) {
        if (order.getCustomer() != null) {
            // markAsCompleted() appelle customer.recordPurchase() — ici on ne fait
            // la mise à jour que si la commande est déjà complète (ex: vente directe)
            if (order.getStatus() == OrderStatus.COMPLETED) {
             //   order.getCustomer().recordPurchase(order.getTotalAmount());
                order.().recordPurchase(order.getTotalAmount())
                customerRepository.save(order.getCustomer());
            }
        }
    }*/

    // =========================================================================
    // VALIDATION
    // =========================================================================

    protected void validateOrderRequest(OrderRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BadRequestException("La commande doit contenir au moins un article");
        }
        if (request.storeId() == null) {
            throw new BadRequestException("Le magasin est obligatoire");
        }
    }
    protected String generateOrderNumber() {
        // ✅ Format: STR{storePrefix}-{yyMMdd}-{séquence 6 chiffres}
        // Utiliser une séquence DB est la meilleure option, mais en attendant :
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyMMdd"));
        // UUID tronqué garantit l'unicité même sous forte charge
        String unique = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
        return "ORD-" + date + "-" + unique;
    }
    // =========================================================================
    // CHARGEMENT DES ENTITÉS
    // =========================================================================

    protected User loadCashier(UUID cashierId) {
        return userRepository.findById(cashierId)
                .orElseThrow(() -> new NotFoundException("Caissier non trouvé: " + cashierId));
    }

    protected Store loadStore(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Magasin non trouvé: " + storeId));
    }

    protected Customer loadCustomer(UUID customerId) {
        if (customerId == null) return null;
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Client non trouvé: " + customerId));
    }

    // =========================================================================
    // GÉNÉRATION DU NUMÉRO DE COMMANDE
    // =========================================================================

    // Dans OrderServiceTemplate.java — remplacer generateOrderNumber()

// Ajouter dans les dépendances du template :
// private final AtomicLong orderSequence = new AtomicLong(System.currentTimeMillis());



//    @NonNull
//    public static String getOrderPrefix(OrderRepository orderRepository) {
//        String prefix    = "ORD";
//        String timestamp = String.valueOf(System.currentTimeMillis());
//        String suffix    = timestamp.substring(timestamp.length() - 6);
//        String random    = String.valueOf((int) (Math.random() * 1000));
//        String number    = prefix + suffix + random;
//
//        while (orderRepository.existsByOrderNumber(number)) {
//            random = String.valueOf((int) (Math.random() * 1000));
//            number = prefix + suffix + random;
//        }
//        return number;
//    }
}