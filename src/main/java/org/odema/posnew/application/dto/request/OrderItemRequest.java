package org.odema.posnew.application.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemRequest(
        UUID productId,
        Integer quantity,
        BigDecimal discountPercentage,
        String notes
) {
}
