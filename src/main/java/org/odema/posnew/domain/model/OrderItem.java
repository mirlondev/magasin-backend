package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.odema.posnew.domain.model.enums.UnitType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_item_order", columnList = "order_id"),
        @Index(name = "idx_item_product", columnList = "product_id")
})
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_item_id", updatable = false, nullable = false)
    private UUID orderItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    // Prix au moment de la commande (snapshot)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal finalPrice;

    @Column(length = 500)
    private String notes;

    @Column(name = "original_price_id")
    private UUID originalPriceId;
    @Enumerated(EnumType.STRING)
    @Column(name = "unit_type", length = 20)
    @Builder.Default
    private UnitType unitType = UnitType.PIECE;

    @Column(name = "unit_quantity", precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal unitQuantity = BigDecimal.ONE;

    // Quantité en unités de BASE (pour le stock)
    @Column(name = "base_quantity", precision = 10, scale = 3)
    private BigDecimal baseQuantity; // calculé automatiquement

    // Mettre à jour calculate() pour calculer baseQuantity
    @PrePersist
    @PreUpdate
    public void calculate() {
        // Quantité en unités de base (pour déduire le stock correctement)
        this.baseQuantity = BigDecimal.valueOf(quantity)
                .multiply(unitQuantity != null ? unitQuantity : BigDecimal.ONE);

        BigDecimal baseTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // ✅ ORDRE CORRECT : remise d'abord, taxe ensuite
        BigDecimal percentDiscount = BigDecimal.ZERO;
        if (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            percentDiscount = baseTotal.multiply(discountPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        BigDecimal fixedDiscount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.discountAmount = percentDiscount.add(fixedDiscount);

        // Net HT après remise
        BigDecimal netHT = baseTotal.subtract(this.discountAmount);

        // Taxe sur le net remisé ✅
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = netHT.multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            this.taxAmount = BigDecimal.ZERO;
        }

        // Prix final TTC
        this.finalPrice = netHT.add(taxAmount);
    }

    public static OrderItem fromStorePrice(Product product, StoreProductPrice price,
                                           int quantity) {
        return OrderItem.builder()
                .product(product)
                .quantity(quantity)
                .unitPrice(price.getBasePrice())
                .taxRate(price.getTaxRate())
                .discountPercentage(price.getDiscountPercentage())
                .unitType(price.getUnitType())
                .unitQuantity(price.getUnitQuantity())
                .originalPriceId(price.getPriceId())
                .build();
        // calculate() sera appelé par @PrePersist
    }

    public BigDecimal getUnitPriceWithTax() {
        return unitPrice.add(
                unitPrice.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
        );
    }

    public BigDecimal getUnitFinalPrice() {
        return quantity > 0 ? finalPrice.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
