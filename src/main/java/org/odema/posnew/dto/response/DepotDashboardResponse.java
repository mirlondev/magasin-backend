package org.odema.posnew.dto.response;

import org.odema.posnew.service.impl.DepotAlertResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DepotDashboardResponse(
        UUID storeId,
        String storeName,
        LocalDate periodStart,
        LocalDate periodEnd,

        // Métriques de dépôt
        Integer totalProductsInDepot,
        BigDecimal totalInventoryValue,
        Integer pendingTransfers,
        Integer completedTransfers,

        // Mouvements de stock
        Map<String, Integer> stockMovements,
        Map<String, BigDecimal> transferValues,

        // Niveaux de stock
        Integer lowStockItems,
        Integer outOfStockItems,
        Integer overStockItems,

        // Commandes fournisseurs
        Integer pendingPurchaseOrders,
        BigDecimal pendingPurchaseValue,

        // Performance du dépôt
        BigDecimal turnoverRate,
        BigDecimal storageUtilization,
        BigDecimal pickingAccuracy,

        // Alertes spécifiques au dépôt
        List<DepotAlertResponse> depotAlerts,

        // Produits populaires
        List<TopProductResponse> popularProducts,

        // Tendances
        Map<String, BigDecimal> inventoryTrend,
        Map<String, Integer> transferTrend
) {}