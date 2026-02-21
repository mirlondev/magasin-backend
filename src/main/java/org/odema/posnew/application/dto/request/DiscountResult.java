package org.odema.posnew.application.dto.request;

import java.math.BigDecimal;

public record DiscountResult(
        BigDecimal originalPrice,
        BigDecimal discountAmount,
        BigDecimal percentageApplied,
        BigDecimal fixedDiscountApplied,
        BigDecimal finalPrice,
        String discountSource,  // "LOYALTY", "PROMOTION", "MANUAL", "BULK"
        String discountReason,
        int loyaltyPointsUsed,
        int loyaltyPointsEarned
) {
}
