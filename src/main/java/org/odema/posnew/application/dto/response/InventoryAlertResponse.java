package org.odema.posnew.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryAlertResponse(
        UUID inventoryId,
        UUID productId,
        String productName,
        Integer currentQuantity,
        Integer reorderPoint,
        Integer minStock,
        Integer maxStock,
        BigDecimal unitCost,
        String alertLevel // LOW, CRITICAL, OUT_OF_STOCK
) {
}
