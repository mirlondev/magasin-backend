package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_sku", columnList = "sku", unique = true),
        @Index(name = "idx_product_barcode", columnList = "barcode", unique = true),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_active", columnList = "is_active"),
        @Index(name = "idx_product_name", columnList = "name")
})
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

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "image_filename")
    private String imageFilename;

    @Column(length = 50, unique = true)
    private String sku;

    @Column(length = 50, unique = true)
    private String barcode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StoreProductPrice> storePrices = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Inventory> inventories = new ArrayList<>();

    // Méthodes métier
    public StoreProductPrice getPriceForStore(UUID storeId) {
        LocalDateTime now = LocalDateTime.now();
        return storePrices.stream()
                .filter(spp -> spp.getStore().getStoreId().equals(storeId))
                .filter(StoreProductPrice::getIsActive)
                .filter(spp -> spp.isValidAt(now))
                .max((a, b) -> a.getEffectiveDate().compareTo(b.getEffectiveDate()))
                .orElse(null);
    }

    public StoreProductPrice getPriceForStoreAt(UUID storeId, LocalDateTime dateTime) {
        return storePrices.stream()
                .filter(spp -> spp.getStore().getStoreId().equals(storeId))
                .filter(StoreProductPrice::getIsActive)
                .filter(spp -> spp.isValidAt(dateTime))
                .max((a, b) -> a.getEffectiveDate().compareTo(b.getEffectiveDate()))
                .orElse(null);
    }

    public java.math.BigDecimal getEffectivePrice(UUID storeId) {
        StoreProductPrice price = getPriceForStore(storeId);
        return price != null ? price.getFinalPrice() : java.math.BigDecimal.ZERO;
    }

    public Integer getTotalStock() {
        return inventories.stream()
                .filter(Inventory::getIsActive)
                .mapToInt(Inventory::getQuantity)
                .sum();
    }

    public Integer getStockInStore(UUID storeId) {
        return inventories.stream()
                .filter(inv -> inv.getStore().getStoreId().equals(storeId))
                .filter(Inventory::getIsActive)
                .mapToInt(Inventory::getQuantity)
                .findFirst()
                .orElse(0);
    }

    public boolean isInStock() {
        return getTotalStock() > 0;
    }

    public boolean isInStockInStore(UUID storeId) {
        return getStockInStore(storeId) > 0;
    }

    public void addStorePrice(StoreProductPrice price) {
        storePrices.add(price);
        price.setProduct(this);
    }

    public void removeStorePrice(StoreProductPrice price) {
        storePrices.remove(price);
        price.setProduct(null);
    }

    public String getFullImageUrl(String baseUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            return imageUrl;
        }
        if (imageFilename != null && !imageFilename.isEmpty()) {
            return baseUrl + "/api/files/products/" + imageFilename;
        }
        return null;
    }
}
