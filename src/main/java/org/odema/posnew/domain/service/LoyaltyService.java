package org.odema.posnew.domain.service;

import org.odema.posnew.application.dto.request.DiscountResult;
import org.odema.posnew.application.dto.response.LoyaltySummaryResponse;
import org.odema.posnew.application.dto.response.LoyaltyTransactionResponse;
import org.odema.posnew.domain.model.enums.LoyaltyTier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface LoyaltyService {

    LoyaltySummaryResponse getCustomerLoyalty(UUID customerId);

    DiscountResult usePointsForDiscount(UUID customerId, int points, UUID orderId);

    void awardPointsForPurchase(UUID customerId, BigDecimal amount, UUID orderId);

    int calculatePointsEarned(BigDecimal amount, LoyaltyTier tier);

    List<LoyaltyTransactionResponse> getTransactionHistory(UUID customerId);

    void adjustPointsManually(UUID customerId, int delta, String reason, UUID adminId);

    BigDecimal calculateTierDiscount(UUID customerId, BigDecimal amount);
}
