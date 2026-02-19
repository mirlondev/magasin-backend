package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entité ShiftReport mise à jour avec toutes les méthodes nécessaires.
 * Cette classe remplace la version précédente de ShiftReport.
 */
@Entity
@Table(name = "shift_reports")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", nullable = false)
    private User cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_register_id", nullable = false)
    private CashRegister cashRegister;

    @Column(name = "opening_time", nullable = false)
    private LocalDateTime openingTime;

    @Column(name = "closing_time")
    private LocalDateTime closingTime;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal actualBalance = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal discrepancy = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer totalTransactions = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRefunds = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netSales = BigDecimal.ZERO;

    // Totaux par méthode de paiement
    @Column(precision = 12, scale = 2)
    private BigDecimal cashSales = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal cardSales = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal mobileMoneySales = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal creditSales = BigDecimal.ZERO;

    // Compteurs par méthode
    private Integer cashSalesCount = 0;
    private Integer cardSalesCount = 0;
    private Integer mobileMoneySalesCount = 0;
    private Integer creditSalesCount = 0;

    // Mouvements de caisse
    @Column(precision = 12, scale = 2)
    private BigDecimal totalCashIn = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalCashOut = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalCancellations = BigDecimal.ZERO;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftStatus status = ShiftStatus.OPEN;

    @OneToMany(mappedBy = "shiftReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Refund> refunds = new ArrayList<>();

    @OneToMany(mappedBy = "shiftReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== MÉTHODES MÉTIER =====

    /**
     * Ferme la session de caisse
     */
    public void closeShift() {
        this.closingTime = LocalDateTime.now();
        this.status = ShiftStatus.CLOSED;
        calculateBalances();
    }

    /**
     * Suspend la session
     */
    public void suspendShift(String reason) {
        this.status = ShiftStatus.SUSPENDED;
        addNote("Session suspendue: " + reason);
    }

    /**
     * Reprend la session suspendue
     */
    public void resumeShift() {
        if (this.status == ShiftStatus.SUSPENDED) {
            this.status = ShiftStatus.OPEN;
            addNote("Session reprise");
        }
    }

    /**
     * Calcule tous les soldes
     */
    public void calculateBalances() {
        this.expectedBalance = openingBalance
                .add(totalSales)
                .subtract(totalRefunds)
                .add(totalCashIn)
                .subtract(totalCashOut);

        if (actualBalance != null) {
            this.discrepancy = actualBalance.subtract(expectedBalance);
        }

        this.netSales = totalSales.subtract(totalRefunds);
        this.closingBalance = expectedBalance;
    }

    /**
     * Ajoute une vente
     */
    public void addSale(BigDecimal amount, PaymentMethod method) {
        this.totalSales = totalSales.add(amount);
        this.totalTransactions++;

        switch (method) {
            case CASH -> {
                this.cashSales = cashSales.add(amount);
                this.cashSalesCount++;
            }
            case CREDIT_CARD -> {
                this.cardSales = cardSales.add(amount);
                this.cardSalesCount++;
            }
            case MOBILE_MONEY -> {
                this.mobileMoneySales = mobileMoneySales.add(amount);
                this.mobileMoneySalesCount++;
            }
            case CREDIT -> {
                this.creditSales = creditSales.add(amount);
                this.creditSalesCount++;
            }
        }

        calculateBalances();
    }

    /**
     * Ajoute un remboursement
     */
    public void addRefund(BigDecimal amount) {
        this.totalRefunds = totalRefunds.add(amount);
        calculateBalances();
    }

    /**
     * Ajoute une entrée de caisse
     */
    public void addCashIn(BigDecimal amount) {
        this.totalCashIn = totalCashIn.add(amount);
        calculateBalances();
    }

    /**
     * Ajoute une sortie de caisse
     */
    public void addCashOut(BigDecimal amount) {
        this.totalCashOut = totalCashOut.add(amount);
        calculateBalances();
    }

    /**
     * Ajoute une annulation
     */
    public void addCancellation(BigDecimal amount) {
        this.totalCancellations = totalCancellations.add(amount);
        calculateBalances();
    }

    /**
     * Définit le solde réel et calcule l'écart
     */
    public void setActualBalanceAndCalculate(BigDecimal actual) {
        this.actualBalance = actual;
        this.discrepancy = actual.subtract(expectedBalance);
    }

    /**
     * Ajoute une note
     */
    public void addNote(String note) {
        if (this.notes == null || this.notes.isEmpty()) {
            this.notes = note;
        } else {
            this.notes += "\n" + note;
        }
    }

    // ===== GETTERS CALCULÉS =====

    public BigDecimal getDifference() {
        if (actualBalance == null || expectedBalance == null) {
            return BigDecimal.ZERO;
        }
        return actualBalance.subtract(expectedBalance);
    }

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

    public int getCashSalesCount() {
        return cashSalesCount != null ? cashSalesCount : 0;
    }

    public int getCardSalesCount() {
        return cardSalesCount != null ? cardSalesCount : 0;
    }

    public int getMobileMoneySalesCount() {
        return mobileMoneySalesCount != null ? mobileMoneySalesCount : 0;
    }

    public int getCreditSalesCount() {
        return creditSalesCount != null ? creditSalesCount : 0;
    }

    // ===== VÉRIFICATIONS =====

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

    // ===== LIFECYCLE =====

    @PrePersist
    public void initDefaults() {
        if (openingBalance == null) openingBalance = BigDecimal.ZERO;
        if (closingBalance == null) closingBalance = BigDecimal.ZERO;
        if (expectedBalance == null) expectedBalance = openingBalance;
        if (actualBalance == null) actualBalance = openingBalance;
        if (discrepancy == null) discrepancy = BigDecimal.ZERO;
        if (totalSales == null) totalSales = BigDecimal.ZERO;
        if (totalRefunds == null) totalRefunds = BigDecimal.ZERO;
        if (netSales == null) netSales = BigDecimal.ZERO;
        if (totalTransactions == null) totalTransactions = 0;
        if (cashSales == null) cashSales = BigDecimal.ZERO;
        if (cardSales == null) cardSales = BigDecimal.ZERO;
        if (mobileMoneySales == null) mobileMoneySales = BigDecimal.ZERO;
        if (creditSales == null) creditSales = BigDecimal.ZERO;
        if (totalCashIn == null) totalCashIn = BigDecimal.ZERO;
        if (totalCashOut == null) totalCashOut = BigDecimal.ZERO;
        if (totalCancellations == null) totalCancellations = BigDecimal.ZERO;
        if (cashSalesCount == null) cashSalesCount = 0;
        if (cardSalesCount == null) cardSalesCount = 0;
        if (mobileMoneySalesCount == null) mobileMoneySalesCount = 0;
        if (creditSalesCount == null) creditSalesCount = 0;
    }
}
