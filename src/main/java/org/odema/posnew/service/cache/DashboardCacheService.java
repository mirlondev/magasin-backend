package org.odema.posnew.service.cache;


import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardCacheService {

    @Cacheable(value = "dashboard-cache", key = "#userId + '-overview-' + #startDate + '-' + #endDate")
    public Object getCachedDashboardOverview(UUID userId, LocalDate startDate, LocalDate endDate) {
        return null; // Le cache retournera null et la méthode réelle sera appelée
    }

    @Cacheable(value = "dashboard-cache", key = "'admin-dashboard-' + #startDate + '-' + #endDate")
    public Object getCachedAdminDashboard(LocalDate startDate, LocalDate endDate) {
        return null;
    }

    @Cacheable(value = "dashboard-cache", key = "#storeId + '-depot-dashboard-' + #startDate + '-' + #endDate")
    public Object getCachedDepotDashboard(UUID storeId, LocalDate startDate, LocalDate endDate) {
        return null;
    }

    @Cacheable(value = "dashboard-cache", key = "#storeId + '-shop-dashboard-' + #startDate + '-' + #endDate")
    public Object getCachedShopDashboard(UUID storeId, LocalDate startDate, LocalDate endDate) {
        return null;
    }

    @Cacheable(value = "dashboard-cache", key = "#storeId + '-sales-chart-' + #period")
    public Object getCachedSalesChart(UUID storeId, String period) {
        return null;
    }
}