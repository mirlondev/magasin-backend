package org.odema.posnew.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PerformanceMetricsResponse(
        LocalDate periodStart,
        LocalDate periodEnd,

        // Métriques de vente
        BigDecimal conversionRate,
        BigDecimal averageBasketSize,
        BigDecimal salesPerSquareMeter,

        // Métriques clients
        BigDecimal customerRetentionRate,
        BigDecimal customerAcquisitionCost,
        BigDecimal customerLifetimeValue,

        // Métriques d'inventaire
        BigDecimal inventoryTurnover,
        BigDecimal grossMarginReturnOnInventory,
        BigDecimal stockoutRate,

        // Métriques employés
        BigDecimal salesPerEmployee,
        BigDecimal transactionsPerEmployee,
        BigDecimal employeeProductivity,

        // KPI financiers
        BigDecimal grossProfitMargin,
        BigDecimal netProfitMargin,
        BigDecimal returnOnInvestment
) {
}
