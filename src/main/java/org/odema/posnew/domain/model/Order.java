package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.domain.model.enums.OrderStatus;
import org.odema.posnew.domain.model.enums.OrderType;
import org.odema.posnew.domain.model.enums.PaymentMethod;
import org.odema.posnew.domain.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_number", columnList = "order_number", unique = true),
        @Index(name = "idx_order_customer", columnList = "customer_id"),
        @Index(name = "idx_order_cashier", columnList = "cashier_id"),
        @Index(name = "idx_order_store", columnList = "store_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_payment", columnList = "payment_status"),
        @Index(name = "idx_order_type", columnList = "order_type"),
        @Index(name = "idx_order_dates", columnList = "created_at,completed_at")
})
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Refund> refunds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderType orderType = OrderType.POS_SALE;

    @Column(length = 500)
    private String notes;

    @Column(name = "global_discount_percentage", precision = 5, scale = 2)
    private BigDecimal globalDiscountPercentage;

    @Column(name = "global_discount_amount", precision = 12, scale = 2)
    private BigDecimal globalDiscountAmount;

    // ✅ NEW: Persisted totalAmount field for queries
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

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

    // ✅ Keep transient methods for runtime calculations
    @Transient
    public BigDecimal getSubtotal() {
        return items.stream()
                .map(OrderItem::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transient
    public BigDecimal getTaxAmount() {
        return items.stream()
                .map(OrderItem::getTaxAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transient
    public BigDecimal getItemsDiscountAmount() {
        return items.stream()
                .map(OrderItem::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transient
    public BigDecimal getGlobalDiscount() {
        BigDecimal subtotal = getSubtotal();
        BigDecimal percentDiscount = BigDecimal.ZERO;

        if (globalDiscountPercentage != null && globalDiscountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            percentDiscount = subtotal.multiply(globalDiscountPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal fixedDiscount = globalDiscountAmount != null ? globalDiscountAmount : BigDecimal.ZERO;
        return percentDiscount.add(fixedDiscount);
    }

    @Transient
    public BigDecimal getTotalDiscount() {
        return getItemsDiscountAmount().add(getGlobalDiscount());
    }

    // ✅ Update persisted totalAmount when called
    @Transient
    public BigDecimal calculateTotalAmount() {
        BigDecimal total = getSubtotal().subtract(getGlobalDiscount());
        this.totalAmount = total;
        return total;
    }

    // ✅ Getter for persisted field
    public BigDecimal getTotalAmount() {
        return totalAmount != null ? totalAmount : calculateTotalAmount();
    }

    @Transient
    public BigDecimal getTotalPaid() {
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .filter(p -> p.getMethod() != PaymentMethod.CREDIT)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transient
    public BigDecimal getTotalCredit() {
        return payments.stream()
                .filter(p -> p.getMethod() == PaymentMethod.CREDIT)
                .filter(p -> p.getStatus() == PaymentStatus.CREDIT)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transient
    public BigDecimal getRemainingAmount() {
        return getTotalAmount().subtract(getTotalPaid()).subtract(getTotalCredit())
                .max(BigDecimal.ZERO);
    }

    @Transient
    public BigDecimal getChangeAmount() {
        BigDecimal paid = getTotalPaid();
        BigDecimal total = getTotalAmount();
        if (paid.compareTo(total) > 0) {
            return paid.subtract(total);
        }
        return BigDecimal.ZERO;
    }

    @Transient
    public PaymentMethod getPrimaryPaymentMethod() {
        return payments.stream()
                .filter(p -> p.getMethod() != PaymentMethod.CREDIT)
                .map(Payment::getMethod)
                .findFirst()
                .orElse(null);
    }

    // ✅ Update totalAmount when adding items
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        calculateTotalAmount();
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);
        calculateTotalAmount();
    }

    public void addPayment(Payment payment) {
        payments.add(payment);
        payment.setOrder(this);
        recalculatePaymentStatus();
    }

    public void removePayment(Payment payment) {
        payments.remove(payment);
        payment.setOrder(null);
        recalculatePaymentStatus();
    }

    public void recalculatePaymentStatus() {
        BigDecimal total = getTotalAmount();
        BigDecimal paid = getTotalPaid();
        BigDecimal credit = getTotalCredit();

        if (paid.compareTo(total) >= 0) {
            this.paymentStatus = PaymentStatus.PAID;
        } else if (credit.compareTo(total) >= 0 && paid.compareTo(BigDecimal.ZERO) == 0) {
            this.paymentStatus = PaymentStatus.CREDIT;
        } else if (paid.add(credit).compareTo(BigDecimal.ZERO) > 0) {
            this.paymentStatus = PaymentStatus.PARTIALLY_PAID;
        } else {
            this.paymentStatus = PaymentStatus.UNPAID;
        }
    }

    public void markAsCompleted() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot complete a cancelled order");
        }
        this.status = OrderStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        calculateTotalAmount(); // Ensure totalAmount is saved
    }

    public void cancel(String reason) {
        if (this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed order");
        }
        this.status = OrderStatus.CANCELLED;
        this.paymentStatus = PaymentStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.notes = (this.notes != null ? this.notes + "\n" : "") + "CANCELLED: " + reason;
    }

    public boolean canAddPayment() {
        return status.canAddPayment() && !paymentStatus.isFinal();
    }

    public boolean isFullyPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    public boolean hasCredit() {
        return getTotalCredit().compareTo(BigDecimal.ZERO) > 0;
    }
}