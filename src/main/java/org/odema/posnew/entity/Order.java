package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.OrderType;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Slf4j
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



    @Column(precision = 12, scale = 2)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private OrderType orderType = OrderType.POS_SALE;

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

    /**
     * Calculate all totals from items - NOT from payments!
     * This ensures order amount is based on products, not money received
     */
    public void calculateTotals() {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }

        // Initialize defaults
        if (this.discountAmount == null) this.discountAmount = BigDecimal.ZERO;
        if (this.taxRate == null) this.taxRate = BigDecimal.ZERO;

        // Calculate subtotal from items (ensuring each item has calculated prices)
        this.subtotal = items.stream()
                .map(item -> {
                    if (item.getFinalPrice() == null) {
                        item.calculatePrices();
                    }
                    return item.getFinalPrice();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate tax
        if (Boolean.TRUE.equals(isTaxable) && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = subtotal.multiply(taxRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            this.taxAmount = BigDecimal.ZERO;
        }

        // Calculate total: subtotal + tax - discount
        this.totalAmount = subtotal.add(taxAmount).subtract(discountAmount);

        // Ensure total is never negative
        if (this.totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.totalAmount = BigDecimal.ZERO;
        }

        // Calculate change based on payments received (for cash handling)
        BigDecimal totalPaid = getTotalPaid();
        if (totalPaid.compareTo(BigDecimal.ZERO) > 0 && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.changeAmount = totalPaid.subtract(totalAmount);
            if (this.changeAmount.compareTo(BigDecimal.ZERO) < 0) {
                this.changeAmount = BigDecimal.ZERO;
            }
        } else {
            this.changeAmount = BigDecimal.ZERO;
        }

        // Update deprecated amountPaid for compatibility
        this.amountPaid = totalPaid;
    }


    /*public BigDecimal getTotalPaid() {
        if (payments == null || payments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .filter(p -> p.getMethod() != PaymentMethod.CREDIT)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }*/
    /**
     * Calculate total paid from payments (excluding credits)
     */
    public BigDecimal getTotalPaid() {
        if (payments == null || payments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)  // Only PAID status
                .filter(p -> p.getMethod() != PaymentMethod.CREDIT) // Exclude CREDIT method
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total paid calculated: {} from {} payments", total, payments.size());
        return total;
    }
    /**
     * Calculate total credit from payments
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
     * Calculate remaining amount to pay
     */
    public BigDecimal getRemainingAmount() {
        BigDecimal paid = getTotalPaid();
        BigDecimal credit = getTotalCredit();
        return totalAmount.subtract(paid).subtract(credit);
    }

    /**
     * Compute payment status based on payments vs total
     */
   /* public PaymentStatus getComputedPaymentStatus() {
        BigDecimal totalPaidAndCredit = getTotalPaid().add(getTotalCredit());

        if (totalPaidAndCredit.compareTo(BigDecimal.ZERO) == 0) {
            return PaymentStatus.UNPAID;
        }

        if (totalPaidAndCredit.compareTo(totalAmount) >= 0) {
            if (getTotalPaid().compareTo(BigDecimal.ZERO) == 0) {
                return PaymentStatus.CREDIT;
            }
            return PaymentStatus.PAID;
        }

        return PaymentStatus.PARTIALLY_PAID;
    }

    /**
 * Compute payment status based on payments vs total
 */
    public PaymentStatus getComputedPaymentStatus() {
        BigDecimal totalPaid = getTotalPaid();
        BigDecimal totalCredit = getTotalCredit();
        BigDecimal totalPaidAndCredit = totalPaid.add(totalCredit);

        log.debug("Computing payment status - Total: {}, Paid: {}, Credit: {}, Combined: {}",
                totalAmount, totalPaid, totalCredit, totalPaidAndCredit);

        if (totalPaidAndCredit.compareTo(BigDecimal.ZERO) == 0) {
            return PaymentStatus.UNPAID;
        }

        // If we have actual payments (cash/card/mobile) that cover the total
        if (totalPaid.compareTo(totalAmount) >= 0) {
            return PaymentStatus.PAID;
        }

        // If we only have credit covering the total
        if (totalCredit.compareTo(totalAmount) >= 0 && totalPaid.compareTo(BigDecimal.ZERO) == 0) {
            return PaymentStatus.CREDIT;
        }

        // Partial payment
        if (totalPaidAndCredit.compareTo(totalAmount) > 0) {
            // Overpayment with mixed methods
            return PaymentStatus.PAID;
        }

        return PaymentStatus.PARTIALLY_PAID;
    }

    /**
     * Add payment to order
     */
    /**
     * Add payment to order
     */
    public void addPayment(Payment payment) {
        if (this.payments == null) {
            this.payments = new ArrayList<>();
        }
        payment.setOrder(this);
        this.payments.add(payment);

        // ✅ ALSO update deprecated field for backward compatibility
        if (this.paymentMethod == null) {
            this.paymentMethod = payment.getMethod();
        } else if (this.paymentMethod != payment.getMethod()) {
            // If multiple payment methods, set to MIXED
            this.paymentMethod = PaymentMethod.MIXED;
        }

        // Recalculate payment status
        this.paymentStatus = getComputedPaymentStatus();

        // Update deprecated amountPaid field
        this.amountPaid = getTotalPaid();

        // Recalculate change
        calculateTotals();
    }
    /**
     * Remove payment from order
     */
    public void removePayment(Payment payment) {
        if (this.payments != null) {
            this.payments.remove(payment);
            payment.setOrder(null);
            this.paymentStatus = getComputedPaymentStatus();
            this.amountPaid = getTotalPaid();
        }
    }

    /**
     * Add item to order
     */
    public void addItem(OrderItem item) {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        item.setOrder(this);
        this.items.add(item);
        calculateTotals();
    }

    /**
     * Remove item from order
     */
    public void removeItem(OrderItem item) {
        if (this.items != null) {
            this.items.remove(item);
            item.setOrder(null);
            calculateTotals();
        }
    }

    /**
     * Mark order as completed
     */
    public void markAsCompleted() {
        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Cancel order
     */
    public void cancelOrder() {
        this.status = OrderStatus.CANCELLED;
        this.paymentStatus = PaymentStatus.REFUNDED;
        this.cancelledAt = LocalDateTime.now();
    }

    /**
     * Check if fully refunded
     */
    public boolean isFullyRefunded() {
        if (this.refunds == null || this.refunds.isEmpty()) {
            return false;
        }
        BigDecimal refundedAmount = refunds.stream()
                .map(Refund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return refundedAmount.compareTo(totalAmount) >= 0;
    }

    // Getters ensuring non-null lists
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

    public String getOriginalOrderId() {
        return orderId != null ? orderId.toString() : null;
    }
}