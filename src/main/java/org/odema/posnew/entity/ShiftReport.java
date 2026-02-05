package org.odema.posnew.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.odema.posnew.entity.enums.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(nullable = false, precision = 12)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12)
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12)
    private BigDecimal expectedBalance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12)
    private BigDecimal actualBalance = BigDecimal.ZERO;

    @Column(precision = 12)
    private BigDecimal discrepancy = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer totalTransactions = 0;

    @Column(nullable = false, precision = 12)
    private BigDecimal totalSales = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12)
    private BigDecimal totalRefunds = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12)
    private BigDecimal netSales = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShiftStatus status = ShiftStatus.OPEN;

    @OneToMany(mappedBy = "shiftReport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Refund> refunds = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Méthodes utilitaires
    public void closeShift() {
        this.endTime = LocalDateTime.now();
        this.status = ShiftStatus.CLOSED;
        calculateBalances();
    }

    public void calculateBalances() {
        // Calculer le solde attendu
        this.expectedBalance = openingBalance.add(totalSales).subtract(totalRefunds);

        // Calculer l'écart
        if (closingBalance != null && actualBalance != null) {
            this.discrepancy = actualBalance.subtract(expectedBalance);
        }

        // Calculer les ventes nettes
        this.netSales = totalSales.subtract(totalRefunds);
    }

    public void addSale(BigDecimal amount) {
        this.totalSales = totalSales.add(amount);
        this.totalTransactions++;
        calculateBalances();
    }

    public void addRefund(BigDecimal amount) {
        this.totalRefunds = totalRefunds.add(amount);
        calculateBalances();
    }

    public boolean isOpen() {
        return status == ShiftStatus.OPEN;
    }

    public boolean isClosed() {
        return status == ShiftStatus.CLOSED;
    }
}
