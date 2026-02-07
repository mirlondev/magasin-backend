package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 10)
    private BigDecimal totalPrice;

    @Column(precision = 5)
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(precision = 10)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10)
    private BigDecimal finalPrice;

    @Column(length = 500)
    private String notes;

    @PrePersist
    public void initializeDefaults() {
        if (discountPercentage == null) discountPercentage = BigDecimal.ZERO;
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
    }

    // Méthode pour calculer les prix
    public void calculatePrices() {
        // Initialize to ZERO if null
        if (this.discountPercentage == null) {
            this.discountPercentage = BigDecimal.ZERO;
        }
        if (this.discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO;
        }

        this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

        if (discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            this.discountAmount = totalPrice.multiply(discountPercentage)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }

        this.finalPrice = totalPrice.subtract(discountAmount);
    }
}