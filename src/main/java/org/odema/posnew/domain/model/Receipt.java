package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.domain.model.enums.ReceiptStatus;
import org.odema.posnew.domain.model.enums.ReceiptType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "receipts", indexes = {
        @Index(name = "idx_receipt_number", columnList = "receipt_number", unique = true),
        @Index(name = "idx_receipt_order", columnList = "order_id"),
        @Index(name = "idx_receipt_shift", columnList = "shift_report_id"),
        @Index(name = "idx_receipt_cashier", columnList = "cashier_id"),
        @Index(name = "idx_receipt_store", columnList = "store_id"),
        @Index(name = "idx_receipt_status", columnList = "status"),
        @Index(name = "idx_receipt_type", columnList = "receipt_type"),
        @Index(name = "idx_receipt_date", columnList = "receipt_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "receipt_id")
    private UUID receiptId;

    @Column(name = "receipt_number", unique = true, nullable = false, length = 50)
    private String receiptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_type", nullable = false, length = 30)
    private ReceiptType receiptType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReceiptStatus status = ReceiptStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_report_id")
    private ShiftReport shiftReport;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "receipt_date", nullable = false)
    private LocalDateTime receiptDate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal amountPaid;

    @Column(precision = 15, scale = 2)
    private BigDecimal changeAmount;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "pdf_filename", length = 255)
    private String pdfFilename;

    @Column(name = "pdf_path", length = 500)
    private String pdfPath;

    @Column(name = "thermal_data", columnDefinition = "TEXT")
    private String thermalData;

    @Column(name = "print_count", nullable = false)
    @Builder.Default
    private Integer printCount = 0;

    @Column(name = "last_printed_at")
    private LocalDateTime lastPrintedAt;

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
        this.printCount = (this.printCount != null ? this.printCount : 0) + 1;
        this.lastPrintedAt = LocalDateTime.now();
    }

    public boolean isVoid() {
        return this.status == ReceiptStatus.VOID;
    }

    public boolean isPrinted() {
        return this.printCount != null && this.printCount > 0;
    }

    public void voidReceipt(String reason) {
        this.status = ReceiptStatus.VOID;
        this.notes = (this.notes != null ? this.notes + "\n" : "") +
                "VOIDED: " + reason + " at " + LocalDateTime.now();
    }
}
