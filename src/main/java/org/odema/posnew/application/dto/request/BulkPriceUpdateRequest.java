package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BulkPriceUpdateRequest(
        @NotNull UUID productId,
        @NotNull UUID storeId,
        List<UUID> productIds,
        @NotNull @Positive BigDecimal newBasePrice,
        @PositiveOrZero BigDecimal taxRate,
        @PositiveOrZero BigDecimal discountPercentage,
        @PositiveOrZero BigDecimal discountAmount,
        @NotNull LocalDateTime effectiveDate,
        LocalDateTime endDate,
        String description
) {
}
