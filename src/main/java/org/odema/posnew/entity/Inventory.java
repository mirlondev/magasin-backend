package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.entity.enums.StockStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inventories",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"product_id", "store_id"})
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 10)
    private BigDecimal unitCost;

    @Column(precision = 10)
    private BigDecimal sellingPrice;

    @Column(name = "reorder_point")
    private Integer reorderPoint = 10;

    @Column(name = "max_stock")
    private Integer maxStock = 1000;

    @Column(name = "min_stock")
    private Integer minStock = 5;

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
    private Boolean isActive = true;

    // Méthodes utilitaires
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
        this.quantity += amount;
        updateStockStatus();
        if (amount > 0) {
            this.lastRestocked = LocalDateTime.now();
        }
    }

    public void decreaseQuantity(int amount) {
        if (this.quantity < amount) {
            throw new IllegalArgumentException("Stock insuffisant. Disponible: " + quantity + ", Demande: " + amount);
        }
        this.quantity -= amount;
        updateStockStatus();
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
        updateStockStatus();
    }

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

    public BigDecimal getTotalValue() {
        return unitCost != null ? unitCost.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
    }
}
