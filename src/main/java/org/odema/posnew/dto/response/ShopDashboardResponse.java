package org.odema.posnew.dto.response;



import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ShopDashboardResponse(
        UUID storeId,
        String storeName,
        LocalDate periodStart,
        LocalDate periodEnd,

        // Métriques de vente
        BigDecimal totalSales,
        Integer totalTransactions,
        Integer totalCustomers,
        BigDecimal averageTransactionValue,

        // Performance des employés
        Map<String, BigDecimal> salesByCashier,
        Map<String, Integer> transactionsByCashier,

        // Métriques de produit
        Integer totalProducts,
        Integer lowStockProducts,
        Integer outOfStockProducts,

        // Performance temporelle
        BigDecimal salesToday,
        BigDecimal salesThisWeek,
        BigDecimal salesThisMonth,

        // Heures de pointe
        Map<String, Integer> peakHours,

        // Métriques clients
        Integer newCustomers,
        Integer returningCustomers,
        BigDecimal customerSatisfaction,

        // Remboursements et retours
        Integer totalRefunds,
        BigDecimal refundAmount,

        // Caisse actuelle
        BigDecimal currentCashBalance,
        BigDecimal expectedCashBalance,
        BigDecimal cashDiscrepancy,

        // Alertes boutique
        List<ShopAlertResponse> shopAlerts
) {}