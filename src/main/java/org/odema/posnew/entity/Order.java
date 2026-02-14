package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", updatable = false, nullable = false)
    private UUID orderId;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

//    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
//    @Builder.Default
//    private List<OrderItem> items = new ArrayList<>();

    // ============ NOUVELLE RELATION PAYMENTS ============
//    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
//    @Builder.Default
//    private List<Payment> payments = new ArrayList<>();
//    // ====================================================

    //new
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();


    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // ⚠️ DEPRECATED - Garder pour compatibilité temporaire
    // À terme, utiliser getTotalPaid() calculé depuis payments
    @Column(precision = 12, scale = 2)
    @Deprecated
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    // ⚠️ DEPRECATED - Le payment method est maintenant dans Payment
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Deprecated
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(length = 500)
    private String notes;

    @Column(name = "is_taxable")
    private Boolean isTaxable = true;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.valueOf(0.0);

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Refund> refunds = new ArrayList<>();

    @PrePersist
    public void initializeDefaults() {
        if (items == null) items = new ArrayList<>();
        if (payments == null) payments = new ArrayList<>();
        if (refunds == null) refunds = new ArrayList<>();
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        if (taxAmount == null) taxAmount = BigDecimal.ZERO;
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;
        if (amountPaid == null) amountPaid = BigDecimal.ZERO;
        if (changeAmount == null) changeAmount = BigDecimal.ZERO;
        if (taxRate == null) taxRate = BigDecimal.ZERO;
        if (isTaxable == null) isTaxable = true;
    }

    // ============ NOUVELLES MÉTHODES DE PAIEMENT ============

    /**
     * Calcule le montant total payé (CASH, MOBILE, CARD)
     * Exclut les crédits
     */
    public BigDecimal getTotalPaid() {
        if (payments == null || payments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .filter(p -> p.getMethod() != PaymentMethod.CREDIT)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcule le montant total en crédit
     */
    public BigDecimal getTotalCredit() {
        if (payments == null || payments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return payments.stream()
                .filter(p -> p.getMethod() == PaymentMethod.CREDIT)
                .filter(p -> p.getStatus() == PaymentStatus.CREDIT)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calcule le montant restant à payer
     */
    public BigDecimal getRemainingAmount() {
        BigDecimal paid = getTotalPaid();
        BigDecimal credit = getTotalCredit();
        return totalAmount.subtract(paid).subtract(credit);
    }

    /**
     * Détermine le statut de paiement calculé automatiquement
     */
    public PaymentStatus getComputedPaymentStatus() {
        BigDecimal totalPaidAndCredit = getTotalPaid().add(getTotalCredit());

        if (totalPaidAndCredit.compareTo(BigDecimal.ZERO) == 0) {
            return PaymentStatus.UNPAID;
        }

        if (totalPaidAndCredit.compareTo(totalAmount) >= 0) {
            // Si tout est en crédit, statut CREDIT, sinon PAID
            if (getTotalPaid().compareTo(BigDecimal.ZERO) == 0) {
                return PaymentStatus.CREDIT;
            }
            return PaymentStatus.PAID;
        }

        return PaymentStatus.PARTIALLY_PAID;
    }

    /**
     * Ajoute un paiement à la commande
     */
    public void addPayment(Payment payment) {
        if (this.payments == null) {
            this.payments = new ArrayList<>();
        }
        payment.setOrder(this);
        this.payments.add(payment);

        // Mettre à jour le statut automatiquement
        this.paymentStatus = getComputedPaymentStatus();

        // Mettre à jour amountPaid pour compatibilité
        this.amountPaid = getTotalPaid();
    }

    /**
     * Retire un paiement de la commande
     */
    public void removePayment(Payment payment) {
        if (this.payments != null) {
            this.payments.remove(payment);
            payment.setOrder(null);

            // Mettre à jour le statut
            this.paymentStatus = getComputedPaymentStatus();
            this.amountPaid = getTotalPaid();
        }
    }

    // ============ MÉTHODES EXISTANTES ============

    public void calculateTotals() {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }

        if (this.discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO;
        }
        if (this.taxRate == null) {
            this.taxRate = BigDecimal.ZERO;
        }

        // Calculer le sous-total
        this.subtotal = items.stream()
                .map(OrderItem::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculer la taxe
        if (Boolean.TRUE.equals(isTaxable) && taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = subtotal.multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else {
            this.taxAmount = BigDecimal.ZERO;
        }

        // Calculer le total
        this.totalAmount = subtotal.add(taxAmount).subtract(discountAmount);

        // Calculer la monnaie (basé sur le total payé)
        BigDecimal totalPaid = getTotalPaid();
        if (totalPaid != null && totalAmount != null) {
            this.changeAmount = totalPaid.subtract(totalAmount);
            if (this.changeAmount.compareTo(BigDecimal.ZERO) < 0) {
                this.changeAmount = BigDecimal.ZERO;
            }
        } else {
            this.changeAmount = BigDecimal.ZERO;
        }
    }

    public void addItem(OrderItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        item.setOrder(this);
        this.items.add(item);
        calculateTotals();
    }

    public void removeItem(OrderItem item) {
        if (this.items != null) {
            this.items.remove(item);
            item.setOrder(null);
            calculateTotals();
        }
    }

    public void markAsPaid() {
        this.paymentStatus = PaymentStatus.PAID;
        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancelOrder() {
        this.status = OrderStatus.CANCELLED;
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.cancelledAt = LocalDateTime.now();
    }

    public boolean isFullyRefunded() {
        if (this.refunds == null || this.refunds.isEmpty()) {
            return false;
        }
        BigDecimal refundedAmount = refunds.stream()
                .map(Refund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return refundedAmount.compareTo(totalAmount) >= 0;
    }

    // Getters that ensure non-null lists
    public List<OrderItem> getItems() {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        return this.items;
    }

    public List<Payment> getPayments() {
        if (this.payments == null) {
            this.payments = new ArrayList<>();
        }
        return this.payments;
    }

    public List<Refund> getRefunds() {
        if (this.refunds == null) {
            this.refunds = new ArrayList<>();
        }
        return this.refunds;
    }
}