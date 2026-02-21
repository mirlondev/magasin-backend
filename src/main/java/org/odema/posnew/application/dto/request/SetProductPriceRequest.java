package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SetProductPriceRequest(
        @NotNull UUID productId,
        @NotNull UUID storeId,
       @NotNull @Positive BigDecimal newBasePrice,
        @PositiveOrZero BigDecimal taxRate,
        @PositiveOrZero BigDecimal discountPercentage,
        @PositiveOrZero BigDecimal discountAmount,
        @NotNull LocalDateTime effectiveDate,
        LocalDateTime endDate,
        String description
) {
}

