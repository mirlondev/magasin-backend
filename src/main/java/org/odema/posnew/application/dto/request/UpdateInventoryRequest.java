package org.odema.posnew.application.dto.request;

import java.math.BigDecimal;

public record UpdateInventoryRequest(
        Integer quantity,
        String operation, // "add", "remove", "set"
        BigDecimal unitCost,
        BigDecimal sellingPrice,
        Integer reorderPoint,
        Integer maxStock,
        Integer minStock,
        String notes
) {
}
