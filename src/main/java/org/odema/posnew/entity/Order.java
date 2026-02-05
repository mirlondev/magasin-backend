package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 12)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 12)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 12)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(precision = 12)
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(length = 500)
    private String notes;

    @Column(name = "is_taxable")
    private Boolean isTaxable = true;

    @Column(name = "tax_rate", precision = 5)
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
    private List<Refund> refunds = new ArrayList<>();

    // Méthodes utilitaires
    public void calculateTotals() {
        // Calculer le sous-total
        this.subtotal = items.stream()
                .map(OrderItem::getFinalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculer la taxe
        if (isTaxable && taxRate != null) {
            this.taxAmount = subtotal.multiply(taxRate)
                    .divide(BigDecimal.valueOf(100));
        }

        // Calculer le total
        this.totalAmount = subtotal.add(taxAmount).subtract(discountAmount);

        // Calculer la monnaie
        if (amountPaid != null && totalAmount != null) {
            this.changeAmount = amountPaid.subtract(totalAmount);
            if (this.changeAmount.compareTo(BigDecimal.ZERO) < 0) {
                this.changeAmount = BigDecimal.ZERO;
            }
        }
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        this.items.add(item);
        calculateTotals();
    }

    public void removeItem(OrderItem item) {
        this.items.remove(item);
        item.setOrder(null);
        calculateTotals();
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
        BigDecimal refundedAmount = refunds.stream()
                .map(Refund::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return refundedAmount.compareTo(totalAmount) >= 0;
    }
}
