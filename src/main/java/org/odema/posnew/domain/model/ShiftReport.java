package org.odema.posnew.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.domain.model.enums.PaymentMethod;
import org.odema.posnew.domain.model.enums.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shift_reports", indexes = {
        @Index(name = "idx_shift_number", columnList = "shift_number", unique = true),
        @Index(name = "idx_shift_cashier", columnList = "cashier_id"),
        @Index(name = "idx_shift_store", columnList = "store_id"),
        @Index(name = "idx_shift_register", columnList = "cash_register_id"),
        @Index(name = "idx_shift_status", columnList = "status"),
        @Index(name = "idx_shift_dates", columnList = "opening_time,closing_time")
})
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "shift_report_id", updatable = false, nullable = false)
    private UUID shiftReportId;

    @Column(nullable = false, unique = true, length = 50)
    private String shiftNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_register_id", nullable = false)
    private CashRegister cashRegister;

    @Column(name = "opening_time", nullable = false)
    private LocalDateTime openingTime;

    @Column(name = "closing_time")
    private LocalDateTime closingTime;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal expectedBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal actualBalance = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discrepancy = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalTransactions = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalRefunds = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal netSales = BigDecimal.ZERO;

    // =========================================================================
    // TOTAUX PAR MÉTHODE DE PAIEMENT (MONTANTS)
    // =========================================================================
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cashSales = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cardSales = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal mobileMoneySales = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal creditSales = BigDecimal.ZERO;

    // =========================================================================
    // ✅ COMPTEURS PAR MÉTHODE DE PAIEMENT (NOUVEAUX CHAMPS)
    // =========================================================================
    @Column(name = "cash_sales_count")
    @Builder.Default
    private Integer cashSalesCount = 0;

    @Column(name = "card_sales_count")
    @Builder.Default
    private Integer cardSalesCount = 0;

    @Column(name = "mobile_money_sales_count")
    @Builder.Default
    private Integer mobileMoneySalesCount = 0;

    @Column(name = "credit_sales_count")
    @Builder.Default
    private Integer creditSalesCount = 0;

    // =========================================================================
    // MOUVEMENTS DE CAISSE
    // =========================================================================
    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCashIn = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCashOut = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalCancellations = BigDecimal.ZERO;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShiftStatus status = ShiftStatus.OPEN;

    @OneToMany(mappedBy = "shiftReport", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @OneToMany(mappedBy = "shiftReport", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Refund> refunds = new ArrayList<>();

    @OneToMany(mappedBy = "shiftReport", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =========================================================================
    // MÉTHODES MÉTIER
    // =========================================================================

    public void calculateBalances() {
        recalculateExpectedBalance();
        if (this.actualBalance != null) {
            this.discrepancy = this.actualBalance.subtract(this.expectedBalance);
            this.closingBalance = this.actualBalance;
        }
    }

    public void closeShift(BigDecimal actualBalance) {
        this.closingTime = LocalDateTime.now();
        this.actualBalance = actualBalance != null ? actualBalance : this.expectedBalance;
        this.discrepancy = this.actualBalance.subtract(this.expectedBalance);
        this.closingBalance = this.actualBalance;
        this.status = ShiftStatus.CLOSED;
    }

    public void suspendShift(String reason) {
        this.status = ShiftStatus.SUSPENDED;
        addNote("SUSPENDED: " + reason);
    }

    public void resumeShift() {
        if (this.status == ShiftStatus.SUSPENDED) {
            this.status = ShiftStatus.OPEN;
            addNote("RESUMED");
        }
    }

    /**
     * ✅ MÉTHODE CORRIGÉE - Incrémente aussi le compteur
     */
    public void addSale(BigDecimal amount, PaymentMethod method) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;

        this.totalSales = this.totalSales.add(amount);
        this.totalTransactions++;

        switch (method) {
            case CASH -> {
                this.cashSales = this.cashSales.add(amount);
                this.cashSalesCount++;
            }
            case CREDIT_CARD -> {
                this.cardSales = this.cardSales.add(amount);
                this.cardSalesCount++;
            }
            case MOBILE_MONEY -> {
                this.mobileMoneySales = this.mobileMoneySales.add(amount);
                this.mobileMoneySalesCount++;
            }
            case CREDIT -> {
                this.creditSales = this.creditSales.add(amount);
                this.creditSalesCount++;
            }
            default -> {}
        }

        recalculateExpectedBalance();
    }

    public void addRefund(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        this.totalRefunds = this.totalRefunds.add(amount);
        recalculateExpectedBalance();
    }

    public void addCashIn(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        this.totalCashIn = this.totalCashIn.add(amount);
        recalculateExpectedBalance();
    }

    public void addCashOut(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        this.totalCashOut = this.totalCashOut.add(amount);
        recalculateExpectedBalance();
    }

    public void addCancellation(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;
        this.totalCancellations = this.totalCancellations.add(amount);
        recalculateExpectedBalance();
    }

    private void recalculateExpectedBalance() {
        this.expectedBalance = this.openingBalance
                .add(this.totalSales)
                .subtract(this.totalRefunds)
                .add(this.totalCashIn)
                .subtract(this.totalCashOut);
        this.netSales = this.totalSales.subtract(this.totalRefunds);
    }

    public void addNote(String note) {
        String timestamp = LocalDateTime.now().toString();
        this.notes = (this.notes != null ? this.notes + "\n" : "") +
                "[" + timestamp + "] " + note;
    }

    // =========================================================================
    // VÉRIFICATIONS
    // =========================================================================

    public boolean isOpen() {
        return status == ShiftStatus.OPEN;
    }

    public boolean isClosed() {
        return status == ShiftStatus.CLOSED;
    }

    public boolean isSuspended() {
        return status == ShiftStatus.SUSPENDED;
    }

    public boolean hasDiscrepancy() {
        return discrepancy != null && discrepancy.compareTo(BigDecimal.ZERO) != 0;
    }

    public BigDecimal getDiscrepancyAmount() {
        return discrepancy != null ? discrepancy : BigDecimal.ZERO;
    }

    // =========================================================================
    // GETTERS SÉCURISÉS - MONTANTS
    // =========================================================================

    public BigDecimal getCashSales() {
        return cashSales != null ? cashSales : BigDecimal.ZERO;
    }

    public BigDecimal getCardSales() {
        return cardSales != null ? cardSales : BigDecimal.ZERO;
    }

    public BigDecimal getMobileMoneySales() {
        return mobileMoneySales != null ? mobileMoneySales : BigDecimal.ZERO;
    }

    public BigDecimal getCreditSales() {
        return creditSales != null ? creditSales : BigDecimal.ZERO;
    }

    public BigDecimal getTotalCashIn() {
        return totalCashIn != null ? totalCashIn : BigDecimal.ZERO;
    }

    public BigDecimal getTotalCashOut() {
        return totalCashOut != null ? totalCashOut : BigDecimal.ZERO;
    }

    public BigDecimal getTotalCancellations() {
        return totalCancellations != null ? totalCancellations : BigDecimal.ZERO;
    }

    public BigDecimal getTotalRefunds() {
        return totalRefunds != null ? totalRefunds : BigDecimal.ZERO;
    }

    public BigDecimal getTotalSales() {
        return totalSales != null ? totalSales : BigDecimal.ZERO;
    }

    public BigDecimal getNetSales() {
        return netSales != null ? netSales : BigDecimal.ZERO;
    }

    // =========================================================================
    // ✅ GETTERS SÉCURISÉS - COMPTEURS (NOUVEAUX)
    // =========================================================================

    public Integer getCashSalesCount() {
        return cashSalesCount != null ? cashSalesCount : 0;
    }

    public Integer getCardSalesCount() {
        return cardSalesCount != null ? cardSalesCount : 0;
    }

    public Integer getMobileMoneySalesCount() {
        return mobileMoneySalesCount != null ? mobileMoneySalesCount : 0;
    }

    public Integer getCreditSalesCount() {
        return creditSalesCount != null ? creditSalesCount : 0;
    }

    public Integer getTotalTransactions() {
        return totalTransactions != null ? totalTransactions : 0;
    }

    // =========================================================================
    // MÉTHODE POUR LE RAPPORT X - Calcul de l'écart
    // =========================================================================
    public BigDecimal getDifference() {
        if (actualBalance == null || expectedBalance == null) {
            return BigDecimal.ZERO;
        }
        return actualBalance.subtract(expectedBalance);
    }
}