package org.odema.posnew.application.dto.response;


import java.time.LocalDateTime;
import java.util.UUID;

public record LoyaltyTransactionResponse(
        UUID transactionId,
        int pointsChange,
        int newBalance,
        String reason,
        UUID orderId,
        LocalDateTime transactionDate
) {

}