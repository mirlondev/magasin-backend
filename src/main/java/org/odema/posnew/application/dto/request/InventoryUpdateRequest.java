package org.odema.posnew.application.dto.request;

import org.odema.posnew.domain.enums_old.StockStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryUpdateRequest(
        Integer quantity,

        String operation, // "add", "remove", "set", "transfer"

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
