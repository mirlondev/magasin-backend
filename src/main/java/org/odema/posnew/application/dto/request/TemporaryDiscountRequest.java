package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TemporaryDiscountRequest(

        @NotNull UUID storeId,
        @NotNull UUID productId,
        @NotNull @Positive BigDecimal newBasePrice,
        @PositiveOrZero BigDecimal taxRate,
        @PositiveOrZero BigDecimal discountPercentage,
        @PositiveOrZero BigDecimal discountAmount,
        @NotNull LocalDateTime effectiveDate,
        LocalDateTime endDate,
        LocalDateTime startDate,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        String description,
        String reason
) {
}
