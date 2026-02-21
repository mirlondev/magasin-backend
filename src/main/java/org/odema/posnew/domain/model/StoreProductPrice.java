package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.domain.model.enums.UnitType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "store_product_prices", indexes = {
        @Index(name = "idx_spp_product_store", columnList = "product_id,store_id,effective_date"),
        @Index(name = "idx_spp_active_dates", columnList = "is_active,effective_date,end_date"),
        @Index(name = "idx_spp_store", columnList = "store_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "store_id", "effective_date"},
                name = "uk_spp_product_store_date")
})
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreProductPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "price_id", updatable = false, nullable = false)
    private UUID priceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal taxRate = new BigDecimal("19.25");

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDateTime effectiveDate;

    @Column
    private LocalDateTime endDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", length = 20)
    @Builder.Default
    private UnitType unitType = UnitType.PIECE;

    @Column(name = "unit_quantity", precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal unitQuantity = BigDecimal.ONE;

    public BigDecimal getPricePerBaseUnit() {
        if (unitQuantity == null || unitQuantity.compareTo(BigDecimal.ZERO) == 0)
            return basePrice;
        return basePrice.divide(unitQuantity, 4, RoundingMode.HALF_UP);
    }

    public BigDecimal getTaxAmount() {
        return basePrice.multiply(taxRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getDiscountValue() {
        BigDecimal percentDiscount = BigDecimal.ZERO;
        if (discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            percentDiscount = basePrice.multiply(discountPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return percentDiscount.max(discountAmount);
    }

    public BigDecimal getFinalPrice() {
        return basePrice.add(getTaxAmount()).subtract(getDiscountValue());
    }

    public BigDecimal getPriceWithoutTax() {
        return basePrice.subtract(getDiscountValue());
    }

    public boolean isValidAt(LocalDateTime dateTime) {
        return !dateTime.isBefore(effectiveDate) &&
                (endDate == null || !dateTime.isAfter(endDate));
    }

    public boolean isCurrentlyValid() {
        return isValidAt(LocalDateTime.now());
    }

    public boolean hasActiveDiscount() {
        return discountPercentage.compareTo(BigDecimal.ZERO) > 0 ||
                discountAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getDiscountPercentageEffective() {
        if (discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            return discountPercentage;
        }
        if (discountAmount.compareTo(BigDecimal.ZERO) > 0 && basePrice.compareTo(BigDecimal.ZERO) > 0) {
            return discountAmount.multiply(BigDecimal.valueOf(100))
                    .divide(basePrice, 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }
}