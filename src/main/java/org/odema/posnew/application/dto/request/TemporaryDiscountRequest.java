package org.odema.posnew.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TemporaryDiscountRequest(
        @NotNull UUID productId,
        @NotNull UUID storeId,
        @NotNull @Positive BigDecimal discountPercentage,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        String reason
) {
}
