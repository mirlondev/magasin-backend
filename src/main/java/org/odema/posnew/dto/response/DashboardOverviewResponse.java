package org.odema.posnew.dto.response;


import org.odema.posnew.entity.enums.UserRole;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record DashboardOverviewResponse(
        UserRole userRole,
        String storeName,
        LocalDate periodStart,
        LocalDate periodEnd,

        // Métriques principales
        BigDecimal totalSales,
        Integer totalOrders,
        Integer totalCustomers,
        Integer totalProducts,
        Integer activeEmployees,

        // Tendances
        BigDecimal salesGrowth,
        BigDecimal orderGrowth,
        BigDecimal customerGrowth,

        // Graphiques
        Map<String, BigDecimal> dailySales,
        Map<String, BigDecimal> weeklySales,
        Map<String, BigDecimal> monthlySales,

        // Alertes
        List<InventoryAlertResponse> lowStockItems,
        List<OrderAlertResponse> pendingOrders,

        // Activités récentes
        List<ActivityLogResponse> recentActivities,

        // Vue spécifique au rôle
        Object roleSpecificData
) {}