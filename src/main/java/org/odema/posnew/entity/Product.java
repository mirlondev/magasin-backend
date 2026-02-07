package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id", updatable = false, nullable = false)
    private UUID productId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10)
    private BigDecimal price;

    /**
     * DEPRECATED: Do not use this field directly!
     * Use getTotalStock() instead, which calculates from Inventory.
     * This field is kept for backward compatibility and database schema.
     * Always set to 0 or sync from inventories.
//     */
//    @Column(nullable = false)
//    @Deprecated
//    private Integer quantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(length = 50)
    private String sku;

    @Column(length = 50)
    private String barcode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inventory> inventories = new ArrayList<>();

    /**
     * Get total stock across ALL stores.
     * This is the CORRECT way to get product quantity.
     *
     * @return Total quantity across all active inventories
     */
    public Integer getTotalStock() {
        if (inventories == null || inventories.isEmpty()) {
            return 0;
        }
        return inventories.stream()
                .filter(Inventory::getIsActive)
                .mapToInt(Inventory::getQuantity)
                .sum();
    }

    /**
     * Check if product is in stock in ANY store.
     *
     * @return true if total stock > 0
     */
    public boolean isInStock() {
        return getTotalStock() > 0;
    }

    /**
     * Get stock quantity in a specific store.
     *
     * @param storeId Store UUID
     * @return Quantity in that store, or 0 if not found
     */
    public Integer getStockInStore(UUID storeId) {
        if (inventories == null || storeId == null) {
            return 0;
        }
        return inventories.stream()
                .filter(inv -> inv.getStore() != null &&
                        inv.getStore().getStoreId().equals(storeId))
                .filter(Inventory::getIsActive)
                .mapToInt(Inventory::getQuantity)
                .findFirst()
                .orElse(0);
    }

    /**
     * Get stock quantity in a specific store by store ID string.
     *
     * @param storeIdString Store UUID as string
     * @return Quantity in that store, or 0 if not found
     */
//    public Integer getStockInStore(String storeIdString) {
//        if (inventories == null || storeIdString == null) {
//            return 0;
//        }
//        return inventories.stream()
//                .filter(inv -> inv.getStore() != null &&
//                        inv.getStore().getStoreId().equals(storeIdString))
//                .filter(Inventory::getIsActive)
//                .mapToInt(Inventory::getQuantity)
//                .findFirst()
//                .orElse(0);
//    }

    /**
     * Check if product is low stock in any store.
     *
     * @return true if any store inventory is low
     */
    public boolean isLowStockAnywhere() {
        if (inventories == null) {
            return false;
        }
        return inventories.stream()
                .filter(Inventory::getIsActive)
                .anyMatch(Inventory::isLowStock);
    }

    /**
     * Check if product is out of stock in any store.
     *
     * @return true if any store inventory is empty
     */
    public boolean isOutOfStockAnywhere() {
        if (inventories == null) {
            return false;
        }
        return inventories.stream()
                .filter(Inventory::getIsActive)
                .anyMatch(Inventory::isOutOfStock);
    }



    /**
     * Sync the quantity field from inventories.
     * Call this if you need to keep the quantity column in sync.
     * Only needed if you can't remove the quantity column yet.
     */
//    public void syncQuantityFromInventories() {
//        this.quantity = getTotalStock();
//    }
}