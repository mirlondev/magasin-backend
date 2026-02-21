package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.domain.model.enums.StockStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventories", indexes = {
        @Index(name = "idx_inventory_product_store", columnList = "product_id,store_id", unique = true),
        @Index(name = "idx_inventory_status", columnList = "stock_status"),
        @Index(name = "idx_inventory_active", columnList = "is_active")
})
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inventory_id", updatable = false, nullable = false)
    private UUID inventoryId;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(precision = 12, scale = 2)
    private BigDecimal unitCost; // Coût unitaire (pour valorisation stock)

    @Column(precision = 12, scale = 2)
    private BigDecimal sellingPrice; // Prix de vente historique (backup si StoreProductPrice indisponible)

    @Column(name = "reorder_point")
    @Builder.Default
    private Integer reorderPoint = 10;

    @Column(name = "max_stock")
    @Builder.Default
    private Integer maxStock = 1000;

    @Column(name = "min_stock")
    @Builder.Default
    private Integer minStock = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", length = 20)
    private StockStatus stockStatus;

    @Column(name = "last_restocked")
    private LocalDateTime lastRestocked;

    @Column(name = "next_restock_date")
    private LocalDateTime nextRestockDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Méthodes métier
    @PrePersist
    @PreUpdate
    public void updateStockStatus() {
        if (quantity <= 0) {
            stockStatus = StockStatus.OUT_OF_STOCK;
        } else if (quantity <= reorderPoint) {
            stockStatus = StockStatus.LOW_STOCK;
        } else if (quantity > maxStock) {
            stockStatus = StockStatus.OVER_STOCK;
        } else {
            stockStatus = StockStatus.IN_STOCK;
        }
    }

    public boolean isLowStock() {
        return quantity <= reorderPoint;
    }

    public boolean isOutOfStock() {
        return quantity <= 0;
    }

    public boolean isOverStock() {
        return quantity > maxStock;
    }

    public void increaseQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        this.quantity += amount;
        this.lastRestocked = LocalDateTime.now();
        updateStockStatus();
    }

    public void decreaseQuantity(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (this.quantity < amount) {
            throw new IllegalStateException(
                    String.format("Insufficient stock: available %d, requested %d", this.quantity, amount)
            );
        }
        this.quantity -= amount;
        updateStockStatus();
    }

    public void setQuantity(int quantity) {
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative");
        this.quantity = quantity;
        updateStockStatus();
    }

    public BigDecimal getInventoryValue() {
        return unitCost != null ?
                unitCost.multiply(BigDecimal.valueOf(quantity)) :
                BigDecimal.ZERO;
    }

    public boolean canFulfill(int requestedQuantity) {
        return this.quantity >= requestedQuantity;
    }

    /**
     * Alias de getInventoryValue() utilisé par InventoryServiceImpl.getInventorySummary()
     */
    public BigDecimal getTotalValue() {
        return getInventoryValue();
    }
}
