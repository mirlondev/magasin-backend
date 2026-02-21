package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refund_items", indexes = {
        @Index(name = "idx_ritem_refund", columnList = "refund_id"),
        @Index(name = "idx_ritem_order_item", columnList = "order_item_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "refund_item_id", updatable = false, nullable = false)
    private UUID refundItemId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refund_id", nullable = false)
    private Refund refund;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem originalOrderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "restocking_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal restockingFee = BigDecimal.ZERO;

    @Column(length = 500)
    private String reason;

    @Column(name = "condition_notes", length = 500)
    private String conditionNotes;

    @Column(name = "is_returned", nullable = false)
    @Builder.Default
    private Boolean isReturned = false;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "is_exchange", nullable = false)
    @Builder.Default
    private Boolean isExchange = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_product_id")
    private Product exchangeProduct;

    @Column(name = "exchange_quantity")
    private Integer exchangeQuantity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Méthodes métier
    public void markAsReturned() {
        this.isReturned = true;
        this.returnedAt = LocalDateTime.now();
    }

    public BigDecimal getNetRefundAmount() {
        return refundAmount.subtract(restockingFee != null ? restockingFee : BigDecimal.ZERO);
    }

    public boolean isExchange() {
        return isExchange != null && isExchange;
    }

    public void setupExchange(Product newProduct, Integer qty) {
        this.isExchange = true;
        this.exchangeProduct = newProduct;
        this.exchangeQuantity = qty;
    }

    @PrePersist
    public void prePersist() {
        if (refundAmount == null && unitPrice != null && quantity != null) {
            this.refundAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
        if (isReturned == null) isReturned = false;
        if (isExchange == null) isExchange = false;
    }
}
