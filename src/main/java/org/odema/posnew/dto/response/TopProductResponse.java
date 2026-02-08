package org.odema.posnew.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TopProductResponse(
        UUID productId,
        String productName,
        String categoryName,
        Integer quantitySold,
        BigDecimal revenue,
        BigDecimal profit,
        Double growthRate
) {
}
