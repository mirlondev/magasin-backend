package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateInventoryRequest(
        @NotNull UUID productId,
        @NotNull UUID storeId,
        @NotNull @PositiveOrZero Integer quantity,
        BigDecimal unitCost,
        BigDecimal sellingPrice,
        @PositiveOrZero Integer reorderPoint,
        @Positive Integer maxStock,
        @PositiveOrZero Integer minStock,
        String notes
) {
}
