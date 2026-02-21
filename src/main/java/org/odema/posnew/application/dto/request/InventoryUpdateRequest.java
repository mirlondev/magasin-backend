package org.odema.posnew.application.dto;

import org.odema.posnew.domain.model.enums.StockStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryUpdateRequest(
        Integer quantity,
        String operation, // "add", "remove", "set"
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
