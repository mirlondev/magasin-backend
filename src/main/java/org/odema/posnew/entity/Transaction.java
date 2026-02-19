package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité Transaction - Enregistre toutes les transactions financières.
 * Centralise les mouvements d'argent pour reporting et traçabilité.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_shift", columnList = "shift_report_id"),
    @Index(name = "idx_transaction_type", columnList = "transaction_type"),
    @Index(name = "idx_transaction_date", columnList = "transaction_date"),
    @Index(name = "idx_transaction_cashier", columnList = "cashier_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID transactionId;

    @Column(name = "transaction_number", nullable = false, unique = true, length = 50)
    private String transactionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refund_id")
    private Refund refund;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_report_id")
    private ShiftReport shiftReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_register_id")
    private CashRegister cashRegister;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reference", length = 100)
    private String reference; // Numéro de chèque, référence virement, etc.

    @Column(name = "is_reconciled", nullable = false)
    @Builder.Default
    private Boolean isReconciled = false;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "is_voided", nullable = false)
    @Builder.Default
    private Boolean isVoided = false;

    @Column(name = "voided_at")
    private LocalDateTime voidedAt;

    @Column(name = "void_reason", length = 500)
    private String voidReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== MÉTHODES MÉTIER =====

    /**
     * Vérifie si la transaction est une entrée d'argent
     */
    public boolean isIncoming() {
        return transactionType != null && transactionType.isPositive();
    }

    /**
     * Vérifie si la transaction est une sortie d'argent
     */
    public boolean isOutgoing() {
        return transactionType != null && transactionType.isNegative();
    }

    /**
     * Marque la transaction comme réconciliée
     */
    public void reconcile() {
        this.isReconciled = true;
        this.reconciledAt = LocalDateTime.now();
    }

    /**
     * Annule la transaction
     */
    public void voidTransaction(String reason) {
        this.isVoided = true;
        this.voidedAt = LocalDateTime.now();
        this.voidReason = reason;
    }

    /**
     * Vérifie si la transaction est annulée
     */
    public boolean isVoided() {
        return isVoided != null && isVoided;
    }

    /**
     * Vérifie si la transaction est active (non annulée)
     */
    public boolean isActive() {
        return !isVoided();
    }

    /**
     * Retourne le montant signé (positif ou négatif selon le type)
     */
    public BigDecimal getSignedAmount() {
        if (transactionType == null) return amount;
        return transactionType.isPositive() ? amount : amount.negate();
    }

    /**
     * Vérifie si c'est une vente
     */
    public boolean isSale() {
        return transactionType != null && transactionType.isSale();
    }

    /**
     * Vérifie si c'est un remboursement
     */
    public boolean isRefund() {
        return transactionType != null && transactionType.isRefund();
    }

    @PrePersist
    public void prePersist() {
        if (transactionDate == null) {
            transactionDate = LocalDateTime.now();
        }
        if (isReconciled == null) {
            isReconciled = false;
        }
        if (isVoided == null) {
            isVoided = false;
        }
    }
}
