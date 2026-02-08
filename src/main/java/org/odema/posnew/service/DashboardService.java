package org.odema.posnew.service;

import org.odema.posnew.dto.response.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface DashboardService {
    DashboardOverviewResponse getDashboardOverview(UUID userId, LocalDate startDate, LocalDate endDate);
    AdminDashboardResponse getAdminDashboard(LocalDate startDate, LocalDate endDate);
    DepotDashboardResponse getDepotManagerDashboard(UUID storeId, LocalDate startDate, LocalDate endDate);
    ShopDashboardResponse getShopManagerDashboard(UUID storeId, LocalDate startDate, LocalDate endDate);

    <FrequentProductResponse> CashierDashboardResponse getCashierDashboard(UUID storeId, UUID cashierId);

    SalesChartResponse getSalesChartData(UUID storeId, LocalDate startDate, LocalDate endDate, String period);
    List<TopProductResponse> getTopProducts(UUID storeId, LocalDate startDate, LocalDate endDate, int limit);
    List<InventoryAlertResponse> getInventoryAlerts(UUID storeId, int threshold);
    PerformanceMetricsResponse getPerformanceMetrics(UUID storeId, LocalDate startDate, LocalDate endDate);
    List<ActivityLogResponse> getRecentActivities(UUID userId);

    // Méthodes utilitaires pour les statistiques
    BigDecimal calculateSalesGrowth(UUID storeId, LocalDate currentStart, LocalDate currentEnd, LocalDate previousStart, LocalDate previousEnd);
    Integer getNewCustomersCount(UUID storeId, LocalDate startDate, LocalDate endDate);
    BigDecimal getAverageOrderValue(UUID storeId, LocalDate startDate, LocalDate endDate);
    Map<String, BigDecimal> getRevenueByCategory(UUID storeId, LocalDate startDate, LocalDate endDate);
}