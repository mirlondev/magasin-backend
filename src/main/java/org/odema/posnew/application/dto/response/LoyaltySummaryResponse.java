package org.odema.posnew.application.dto.response;

import org.odema.posnew.domain.model.enums.LoyaltyTier;

import java.math.BigDecimal;
import java.util.UUID;

public record LoyaltySummaryResponse(
        UUID customerId,
        String customerName,
        Integer currentPoints,
        LoyaltyTier currentTier,
        BigDecimal tierDiscountRate,
        Integer pointsToNextTier,
        BigDecimal totalPurchases,
        Integer purchaseCount,
        BigDecimal availableDiscountValue  // Valeur des points en monnaie
) {
}
