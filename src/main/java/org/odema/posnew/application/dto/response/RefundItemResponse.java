package org.odema.posnew.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RefundItemResponse(
        UUID refundItemId,
        UUID originalOrderItemId,
        UUID productId,
        String productName,
        String productSku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal refundAmount,
        BigDecimal restockingFee,
        BigDecimal netRefundAmount,
        String reason,
        Boolean isReturned,
        LocalDateTime returnedAt,
        Boolean isExchange,
        UUID exchangeProductId,
        String exchangeProductName,
        Integer exchangeQuantity
) {
}
