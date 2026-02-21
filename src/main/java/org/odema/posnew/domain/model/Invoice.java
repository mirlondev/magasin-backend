package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.domain.model.enums.InvoiceStatus;
import org.odema.posnew.domain.model.enums.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoices", indexes = {
        @Index(name = "idx_invoice_number", columnList = "invoice_number", unique = true),
        @Index(name = "idx_invoice_order", columnList = "order_id"),
        @Index(name = "idx_invoice_customer", columnList = "customer_id"),
        @Index(name = "idx_invoice_store", columnList = "store_id"),
        @Index(name = "idx_invoice_status", columnList = "status"),
        @Index(name = "idx_invoice_type", columnList = "invoice_type"),
        @Index(name = "idx_invoice_dates", columnList = "invoice_date,payment_due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "invoice_number", unique = true, nullable = false, length = 50)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 30)
    @Builder.Default
    private InvoiceType invoiceType = InvoiceType.CREDIT_SALE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(name = "invoice_date", nullable = false)
    private LocalDateTime invoiceDate;

    @Column(name = "payment_due_date")
    private LocalDateTime paymentDueDate;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 15, scale = 2)
    private BigDecimal taxAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal amountDue;

    @Column(length = 30)
    private String paymentMethod;

    @Column(name = "pdf_filename", length = 255)
    private String pdfFilename;

    @Column(name = "pdf_path", length = 500)
    private String pdfPath;

    @Column(name = "print_count", nullable = false)
    @Builder.Default
    private Integer printCount = 0;

    @Column(name = "last_printed_at")
    private LocalDateTime lastPrintedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean convertedToSale = false;

    @Column(name = "converted_at")
    private LocalDateTime convertedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_order_id")
    private Order convertedOrder;

    @Column(columnDefinition = "TEXT")
    private String notes;

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
    public void incrementPrintCount() {
        this.printCount++;
        this.lastPrintedAt = LocalDateTime.now();
    }

    public boolean isPaid() {
        return this.status == InvoiceStatus.PAID;
    }

    public boolean isOverdue() {
        return this.status == InvoiceStatus.OVERDUE ||
                (paymentDueDate != null && paymentDueDate.isBefore(LocalDateTime.now()) && !isPaid());
    }

    public boolean isProforma() {
        return this.invoiceType.isProforma();
    }

    public boolean canBeConverted() {
        return isProforma() && !convertedToSale &&
                (status == InvoiceStatus.DRAFT || status == InvoiceStatus.ISSUED);
    }

    public void markAsConverted(Order newOrder) {
        this.convertedToSale = true;
        this.convertedAt = LocalDateTime.now();
        this.convertedOrder = newOrder;
        this.status = InvoiceStatus.CONVERTED;
    }

    public void markAsPaid() {
        this.status = InvoiceStatus.PAID;
        this.amountPaid = this.totalAmount;
        this.amountDue = BigDecimal.ZERO;
    }

    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(amountPaid);
    }
}
