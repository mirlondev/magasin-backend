package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.domain.model.enums.RefundMethod;
import org.odema.posnew.domain.model.enums.RefundStatus;
import org.odema.posnew.domain.model.enums.RefundType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "refunds", indexes = {
        @Index(name = "idx_refund_number", columnList = "refund_number", unique = true),
        @Index(name = "idx_refund_order", columnList = "order_id"),
        @Index(name = "idx_refund_status", columnList = "status"),
        @Index(name = "idx_refund_store", columnList = "store_id"),
        @Index(name = "idx_refund_shift", columnList = "shift_report_id"),
        @Index(name = "idx_refund_dates", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor(force = true)
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
    private Order originalOrder;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "total_refund_amount", precision = 12, scale = 2)
    private BigDecimal totalRefundAmount;

    @Column(name = "restocking_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal restockingFee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RefundType refundType = RefundType.FULL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RefundStatus status = RefundStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", length = 20)
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



    // Méthodes métier
    public void addItem(RefundItem item) {
        items.add(item);
        item.setRefund(this);
        recalculateTotals();
    }

    public void removeItem(RefundItem item) {
        items.remove(item);
        item.setRefund(null);
        recalculateTotals();
    }

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

    public void approve(UUID approverId) {
        validateStatusTransition(RefundStatus.APPROVED);
        this.status = RefundStatus.APPROVED;
        this.approvedBy = approverId;
        this.approvedAt = LocalDateTime.now();
    }

    public void startProcessing(UUID processorId) {
        validateStatusTransition(RefundStatus.PROCESSING);
        this.status = RefundStatus.PROCESSING;
        this.processedBy = processorId;
        this.processedAt = LocalDateTime.now();
    }

    public void complete() {
        validateStatusTransition(RefundStatus.COMPLETED);
        this.status = RefundStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        validateStatusTransition(RefundStatus.REJECTED);
        this.status = RefundStatus.REJECTED;
        this.rejectionReason = reason;
        addNote("REJECTED: " + reason);
    }

    public void cancel(String reason) {
        validateStatusTransition(RefundStatus.CANCELLED);
        this.status = RefundStatus.CANCELLED;
        this.isActive = false;
        addNote("CANCELLED: " + reason);
    }

    public void fail(String reason) {
        this.status = RefundStatus.FAILED;
        addNote("FAILED: " + reason);
    }

    private void validateStatusTransition(RefundStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", this.status, newStatus)
            );
        }
    }

    private void addNote(String note) {
        this.notes = (this.notes != null ? this.notes + "\n" : "") +
                "[" + LocalDateTime.now() + "] " + note;
    }

    public boolean isEditable() {
        return status == RefundStatus.PENDING;
    }

    public boolean isFinalized() {
        return status.isFinal();
    }

    public boolean isFullRefund() {
        return refundType == RefundType.FULL;
    }

    public boolean isExchange() {
        return refundType == RefundType.EXCHANGE;
    }

    public BigDecimal getNetAmount() {
        return totalRefundAmount != null ? totalRefundAmount :
                (refundAmount != null ? refundAmount.subtract(restockingFee) : BigDecimal.ZERO);
    }

    public int getTotalItemsCount() {
        return items.stream().mapToInt(RefundItem::getQuantity).sum();
    }

    @PrePersist
    public void prePersist() {
        if (totalRefundAmount == null && refundAmount != null) {
            totalRefundAmount = refundAmount.subtract(restockingFee);
        }
    }
}
