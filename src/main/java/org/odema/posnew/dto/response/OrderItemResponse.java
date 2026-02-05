package org.odema.posnew.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID orderItemId,
        UUID productId,
        String productName,
        String productSku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        BigDecimal discountPercentage,
        BigDecimal discountAmount,
        BigDecimal finalPrice,
        String notes
) {
}
