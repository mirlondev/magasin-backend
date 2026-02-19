package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.entity.enums.RefundMethod;
import org.odema.posnew.entity.enums.RefundStatus;
import org.odema.posnew.entity.enums.RefundType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité Refund - Gère les remboursements de commandes.
 * Un remboursement peut concerner tout ou partie d'une commande.
 */
@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refund_order", columnList = "order_id"),
    @Index(name = "idx_refund_status", columnList = "status"),
    @Index(name = "idx_refund_number", columnList = "refund_number", unique = true),
    @Index(name = "idx_refund_date", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "refund_id", updatable = false, nullable = false)
    private UUID refundId;

    @Column(nullable = false, unique = true, length = 50)
    private String refundNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_order_id")
    private Order originalOrder; // Pour traçabilité

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "total_refund_amount", precision = 12, scale = 2)
    private BigDecimal totalRefundAmount;

    @Column(name = "restocking_fee", precision = 12, scale = 2)
    private BigDecimal restockingFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundType refundType = RefundType.FULL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status = RefundStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", length = 30)
    private RefundMethod refundMethod;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

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

    @OneToMany(mappedBy = "refund", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RefundItem> items = new ArrayList<>();

    @Column(length = 1000)
    private String notes;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== MÉTHODES MÉTIER =====

    /**
     * Ajoute un article au remboursement
     */
    public void addItem(RefundItem item) {
        items.add(item);
        item.setRefund(this);
        recalculateTotals();
    }

    /**
     * Retire un article du remboursement
     */
    public void removeItem(RefundItem item) {
        items.remove(item);
        item.setRefund(null);
        recalculateTotals();
    }

    /**
     * Recalcule les totaux du remboursement
     */
    public void recalculateTotals() {
        BigDecimal itemsTotal = items.stream()
                .map(RefundItem::getRefundAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal restocking = items.stream()
                .map(RefundItem::getRestockingFee)
                .filter(fee -> fee != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.refundAmount = itemsTotal;
        this.restockingFee = restocking;
        this.totalRefundAmount = itemsTotal.subtract(restocking);
    }

    /**
     * Approuve le remboursement
     */
    public void approveRefund(UUID approverId) {
        if (this.status != RefundStatus.PENDING) {
            throw new IllegalStateException("Seuls les remboursements en attente peuvent être approuvés");
        }
        this.status = RefundStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * Démarre le traitement du remboursement
     */
    public void startProcessing(UUID processorId) {
        if (this.status != RefundStatus.APPROVED) {
            throw new IllegalStateException("Le remboursement doit être approuvé avant traitement");
        }
        this.status = RefundStatus.PROCESSING;
        this.processedBy = processorId;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * Marque le remboursement comme terminé
     */
    public void completeRefund() {
        if (this.status != RefundStatus.PROCESSING) {
            throw new IllegalStateException("Le remboursement doit être en cours de traitement");
        }
        this.status = RefundStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Rejette le remboursement
     */
    public void rejectRefund(String reason) {
        if (this.status.isFinal()) {
            throw new IllegalStateException("Impossible de rejeter un remboursement finalisé");
        }
        this.status = RefundStatus.REJECTED;
        this.rejectionReason = reason;
        this.notes = (this.notes != null ? this.notes + "\n" : "") +
                "Rejeté: " + reason;
    }

    /**
     * Annule le remboursement
     */
    public void cancelRefund(String reason) {
        if (this.status.isFinal()) {
            throw new IllegalStateException("Impossible d'annuler un remboursement finalisé");
        }
        this.status = RefundStatus.CANCELLED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") +
                "Annulé: " + reason;
        this.isActive = false;
    }

    /**
     * Marque le remboursement comme échoué
     */
    public void markAsFailed(String failureReason) {
        this.status = RefundStatus.FAILED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") +
                "Échec: " + failureReason;
    }

    /**
     * Vérifie si le remboursement peut être modifié
     */
    public boolean isEditable() {
        return status == RefundStatus.PENDING;
    }

    /**
     * Vérifie si le remboursement est finalisé
     */
    public boolean isFinalized() {
        return status.isFinal();
    }

    /**
     * Vérifie si c'est un remboursement total
     */
    public boolean isFullRefund() {
        return refundType == RefundType.FULL;
    }

    /**
     * Vérifie si c'est un échange
     */
    public boolean isExchange() {
        return refundType == RefundType.EXCHANGE;
    }

    /**
     * Retourne le montant net après frais de restockage
     */
    public BigDecimal getNetAmount() {
        BigDecimal restocking = restockingFee != null ? restockingFee : BigDecimal.ZERO;
        return refundAmount.subtract(restocking);
    }

    /**
     * Compte le nombre d'articles remboursés
     */
    public int getTotalItemsCount() {
        return items.stream()
                .mapToInt(RefundItem::getQuantity)
                .sum();
    }

    @PrePersist
    public void prePersist() {
        if (totalRefundAmount == null && refundAmount != null) {
            BigDecimal restocking = restockingFee != null ? restockingFee : BigDecimal.ZERO;
            totalRefundAmount = refundAmount.subtract(restocking);
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}
