package org.odema.posnew.dto.response;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CashierDashboardResponse<FrequentProductResponse, QuickActionResponse>(
        UUID cashierId,
        String cashierName,
        UUID storeId,
        String storeName,

        // Shift actuel
        UUID currentShiftId,
        LocalDateTime shiftStartTime,
        BigDecimal shiftOpeningBalance,
        BigDecimal currentBalance,
        Integer transactionsInShift,
        BigDecimal salesInShift,

        // Métriques de performance
        BigDecimal averageTransactionTime,
        Integer productsScannedToday,
        Integer customersServedToday,

        // Commandes en attente
        List<OrderResponse> pendingOrders,
        List<OrderResponse> processingOrders,

        // Alertes caisse
        List<CashierAlertResponse> cashierAlerts,

        // Objectifs
        BigDecimal dailySalesTarget,
        BigDecimal currentSalesProgress,
        Integer dailyTransactionTarget,
        Integer currentTransactionProgress,

        // Transactions récentes
        List<TransactionSummaryResponse> recentTransactions,

        // Produits fréquents
        List<FrequentProductResponse> frequentProducts,

        // Mode rapide
        List<QuickActionResponse> quickActions
) {}
