package org.odema.posnew.application.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundItemRequest(
        UUID originalOrderItemId,
        UUID productId,
        Integer quantity,
        BigDecimal refundAmount,
        BigDecimal restockingFee,
        String reason,
        Boolean isReturned,
        Boolean isExchange,
        UUID exchangeProductId,
        Integer exchangeQuantity
) {
}
