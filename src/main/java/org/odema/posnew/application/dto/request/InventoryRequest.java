package org.odema.posnew.application.dto;

import org.odema.posnew.domain.model.enums.StockStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryRequest(
        UUID productId,
        UUID storeId,
        Integer quantity,
        BigDecimal unitCost,
        BigDecimal sellingPrice,
        Integer reorderPoint,
        Integer maxStock,
        Integer minStock,
        StockStatus stockStatus,
        LocalDateTime nextRestockDate,
        String notes
) {
}
