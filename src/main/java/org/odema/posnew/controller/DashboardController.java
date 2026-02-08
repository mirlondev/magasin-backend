package org.odema.posnew.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.response.*;
import org.odema.posnew.entity.enums.UserRole;
import org.odema.posnew.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "API de tableau de bord")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir l'aperçu du dashboard selon le profil")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getDashboardOverview(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        UUID userId = extractUserId(userDetails); // À implémenter selon votre structure
        DashboardOverviewResponse response = dashboardService.getDashboardOverview(userId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dashboard Admin - Vue système")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AdminDashboardResponse response = dashboardService.getAdminDashboard(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/depot-manager/{storeId}")
    @PreAuthorize("hasRole('DEPOT_MANAGER')")
    @Operation(summary = "Dashboard Manager de Dépôt")
    public ResponseEntity<ApiResponse<DepotDashboardResponse>> getDepotManagerDashboard(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        DepotDashboardResponse response = dashboardService.getDepotManagerDashboard(storeId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/shop-manager/{storeId}")
    @PreAuthorize("hasRole('SHOP_MANAGER')")
    @Operation(summary = "Dashboard Manager de Boutique")
    public ResponseEntity<ApiResponse<ShopDashboardResponse>> getShopManagerDashboard(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        ShopDashboardResponse response = dashboardService.getShopManagerDashboard(storeId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/cashier/{storeId}")
    @PreAuthorize("hasAnyRole('CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Dashboard Caissier")
    public ResponseEntity<ApiResponse<CashierDashboardResponse>> getCashierDashboard(
            @PathVariable UUID storeId,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        UUID cashierId = extractUserId(userDetails);
        CashierDashboardResponse response = dashboardService.getCashierDashboard(storeId, cashierId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sales-chart/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'DEPOT_MANAGER')")
    @Operation(summary = "Données pour graphique de ventes")
    public ResponseEntity<ApiResponse<SalesChartResponse>> getSalesChartData(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DAILY") String period) {

        SalesChartResponse response = dashboardService.getSalesChartData(storeId, startDate, endDate, period);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/top-products/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'DEPOT_MANAGER')")
    @Operation(summary = "Top produits vendus")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getTopProducts(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {

        List<TopProductResponse> response = dashboardService.getTopProducts(storeId, startDate, endDate, limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inventory-alerts/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Alertes de stock")
    public ResponseEntity<ApiResponse<List<InventoryAlertResponse>>> getInventoryAlerts(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "10") int threshold) {

        List<InventoryAlertResponse> response = dashboardService.getInventoryAlerts(storeId, threshold);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/performance-metrics/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Métriques de performance")
    public ResponseEntity<ApiResponse<PerformanceMetricsResponse>> getPerformanceMetrics(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        PerformanceMetricsResponse response = dashboardService.getPerformanceMetrics(storeId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/recent-activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Activités récentes")
    public ResponseEntity<ApiResponse<List<ActivityLogResponse>>> getRecentActivities(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        List<ActivityLogResponse> response = dashboardService.getRecentActivities(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private UUID extractUserId(org.springframework.security.core.userdetails.UserDetails userDetails) {
        // Implémentez la logique pour extraire l'ID utilisateur
        // Cela dépend de la façon dont vous stockez l'ID dans UserDetails
        return UUID.fromString(userDetails.getUsername()); // Exemple
    }
}