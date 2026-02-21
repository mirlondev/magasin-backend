package org.odema.posnew.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de réponse pour un article remboursé.
 */
public record RefundItemResponse(
        UUID refundItemId,
        UUID orderItemId,
        UUID productId,
        String productName,
        String productSku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal refundAmount,
        BigDecimal restockingFee,
        BigDecimal netAmount,
        String reason,
        Boolean isReturned,
        LocalDateTime returnedAt,
        Boolean isExchange,
        UUID exchangeProductId,
        String exchangeProductName,
        Integer exchangeQuantity
) {
    /**
     * Constructeur avec calcul du net
     */
    public RefundItemResponse {
        if (refundAmount != null && restockingFee != null) {
            netAmount = refundAmount.subtract(restockingFee);
        } else if (refundAmount != null) {
            netAmount = refundAmount;
        } else {
            netAmount = BigDecimal.ZERO;
        }
    }
}
