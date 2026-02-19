package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité représentant un article remboursé dans un remboursement.
 * Chaque RefundItem est lié à un Refund et à un OrderItem original.
 */
@Entity
@Table(name = "refund_items")
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
    private BigDecimal restockingFee;

    @Column(name = "reason", length = 500)
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

    // ===== MÉTHODES MÉTIER =====

    /**
     * Marque l'article comme retourné
     */
    public void markAsReturned() {
        this.isReturned = true;
        this.returnedAt = LocalDateTime.now();
    }

    /**
     * Calcule le montant net du remboursement (après frais de restockage)
     */
    public BigDecimal getNetRefundAmount() {
        BigDecimal restocking = restockingFee != null ? restockingFee : BigDecimal.ZERO;
        return refundAmount.subtract(restocking);
    }

    /**
     * Vérifie si c'est un échange
     */
    public boolean isExchange() {
        return isExchange != null && isExchange;
    }

    /**
     * Configure comme échange
     */
    public void setupExchange(Product newProduct, Integer qty) {
        this.isExchange = true;
        this.exchangeProduct = newProduct;
        this.exchangeQuantity = qty;
    }

    /**
     * Calcule le montant du remboursement basé sur la quantité
     */
    public void calculateRefundAmount() {
        if (unitPrice != null && quantity != null) {
            this.refundAmount = unitPrice.multiply(new BigDecimal(quantity));
        }
    }

    @PrePersist
    public void prePersist() {
        if (refundAmount == null) {
            calculateRefundAmount();
        }
        if (isReturned == null) {
            isReturned = false;
        }
        if (isExchange == null) {
            isExchange = false;
        }
    }
}
