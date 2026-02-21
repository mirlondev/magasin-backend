package org.odema.posnew.application.dto.response;

import java.math.BigDecimal;

public record InventorySummaryProjection(
        Long totalProducts,
        Long lowStockProducts,
        Long outOfStockProducts,
        Long totalQuantity,
        BigDecimal totalValue
) {
}
