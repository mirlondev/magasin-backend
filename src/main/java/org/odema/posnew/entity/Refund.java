package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.odema.posnew.entity.enums.RefundStatus;
import org.odema.posnew.entity.enums.RefundType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refunds")
@Setter
@Getter
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 12)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundType refundType = RefundType.FULL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status = RefundStatus.PENDING;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_report_id")
    private ShiftReport shiftReport;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    // Méthodes utilitaires
    public void approveRefund() {
        this.status = RefundStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }

    public void completeRefund() {
        this.status = RefundStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void rejectRefund(String rejectionReason) {
        this.status = RefundStatus.REJECTED;
        this.notes = (this.notes != null ? this.notes + "\n" : "") +
                "Rejeté: " + rejectionReason;
    }
}
