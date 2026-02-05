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
@Setter @Getter
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

    @Column(nullable = false)
    private Integer quantity;

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

    public boolean isInStock() {
        return quantity != null && quantity > 0;
    }

    public void decreaseQuantity(int amount) {
        if (this.quantity >= amount) {
            this.quantity -= amount;
        } else {
            throw new IllegalArgumentException("Stock insuffisant");
        }
    }

    public void increaseQuantity(int amount) {
        this.quantity += amount;
    }

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inventory> inventories = new ArrayList<>();

    // Méthode pour obtenir le stock total dans tous les stores
    public Integer getTotalStock() {
        return inventories.stream()
                .filter(Inventory::getIsActive)
                .mapToInt(Inventory::getQuantity)
                .sum();
    }

    // Méthode pour obtenir le stock dans un store spécifique
    public Integer getStockInStore(UUID storeId) {
        return inventories.stream()
                .filter(inv -> inv.getStore().getStoreId().equals(storeId.toString()))
                .filter(Inventory::getIsActive)
                .mapToInt(Inventory::getQuantity)
                .findFirst()
                .orElse(0);
    }
}
