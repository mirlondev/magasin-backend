package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ApplyDiscountRequest(
        @NotNull UUID orderId,
        UUID customerId,
        BigDecimal manualDiscountPercentage,
        BigDecimal manualDiscountAmount,
        String discountReason,
        boolean applyLoyaltyDiscount
) {
}
