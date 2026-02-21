package org.odema.posnew.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InventorySummaryResponse(
        UUID storeId,
        String storeName,
        Long totalProducts,
        Long lowStockProducts,
        Long outOfStockProducts,
        Integer totalQuantity,
        BigDecimal totalValue,
        LocalDateTime generatedAt
) {
}
