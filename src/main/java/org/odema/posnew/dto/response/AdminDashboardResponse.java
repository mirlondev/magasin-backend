package org.odema.posnew.dto.response;



import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AdminDashboardResponse(
        // Vue système
        Integer totalStores,
        Integer activeStores,
        Integer totalUsers,
        Integer activeUsers,

        // Performance globale
        BigDecimal totalRevenue,
        BigDecimal averageOrderValue,
        BigDecimal totalExpenses,
        BigDecimal netProfit,

        // Par type de store
        Map<String, BigDecimal> revenueByStoreType,
        Map<String, Integer> ordersByStoreType,

        // Top performers
        List<StorePerformanceResponse> topPerformingStores,
        List<UserPerformanceResponse> topPerformingUsers,

        // Alertes système
        List<SystemAlertResponse> systemAlerts,

        // Statistiques temporelles
        Map<String, BigDecimal> revenueTrend,
        Map<String, Integer> userGrowth,

        // Métriques financières
        BigDecimal cashFlow,
        BigDecimal inventoryValue,
        BigDecimal accountsReceivable,
        BigDecimal accountsPayable
) {}