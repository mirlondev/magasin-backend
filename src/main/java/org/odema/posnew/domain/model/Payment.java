package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.odema.posnew.domain.model.enums.PaymentMethod;
import org.odema.posnew.domain.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order", columnList = "order_id"),
        @Index(name = "idx_payment_shift", columnList = "shift_report_id"),
        @Index(name = "idx_payment_cashier", columnList = "cashier_id"),
        @Index(name = "idx_payment_method", columnList = "method"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_date", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID paymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_report_id")
    private ShiftReport shiftReport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PAID;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    public boolean isCredit() {
        return method == PaymentMethod.CREDIT;
    }

    public boolean isActualPayment() {
        return status == PaymentStatus.PAID && method != PaymentMethod.CREDIT;
    }

    public void cancel(String reason) {
        this.status = PaymentStatus.CANCELLED;
        this.isActive = false;
        this.cancelledAt = LocalDateTime.now();
        this.notes = (this.notes != null ? this.notes + "\n" : "") + "CANCELLED: " + reason;
    }

    public boolean canBeCancelled() {
        return isActive && status != PaymentStatus.CANCELLED;
    }
}