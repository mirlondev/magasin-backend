package org.odema.posnew.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InventorySummaryResponse(
        UUID storeId,
        String storeName,
        long totalProducts,
        long lowStockProducts,
        long outOfStockProducts,
        int totalQuantity,
        BigDecimal totalValue,
        LocalDateTime generatedAt
) {
}
