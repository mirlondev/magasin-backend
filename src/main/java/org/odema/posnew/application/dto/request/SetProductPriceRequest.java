package org.odema.posnew.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SetProductPriceRequest(
        @NotNull UUID productId,
        @NotNull UUID storeId,
        @NotNull @Positive BigDecimal basePrice,
        @PositiveOrZero BigDecimal taxRate,
        @PositiveOrZero BigDecimal discountPercentage,
        @PositiveOrZero BigDecimal discountAmount,
        @NotNull LocalDateTime effectiveDate,
        LocalDateTime endDate,
        String description
) {
}
