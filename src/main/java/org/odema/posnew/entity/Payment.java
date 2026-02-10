package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité Payment - Gère tous les paiements (cash, crédit, mobile, etc.)
 * Chaque paiement est lié à une commande et un shift report
 */
@Entity
@Table(name = "payments")
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
    private PaymentMethod method; // CASH, MOBILE, CARD, CREDIT

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
    private PaymentStatus status; // PAID, PARTIAL, CREDIT, CANCELLED

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Vérifie si le paiement est un crédit
     */
    public boolean isCredit() {
        return method == PaymentMethod.CREDIT;
    }

    /**
     * Vérifie si le paiement a été effectivement payé (pas un crédit)
     */
    public boolean isActualPayment() {
        return status == PaymentStatus.PAID && method != PaymentMethod.CREDIT;
    }

    /**
     * Annule le paiement
     */
    public void cancel() {
        this.status = PaymentStatus.CANCELLED;
        this.isActive = false;
        this.cancelledAt = LocalDateTime.now();
    }
}