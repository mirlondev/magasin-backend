package org.odema.posnew.application.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdatePriceRequest(
        BigDecimal basePrice,
        BigDecimal taxRate,
        BigDecimal discountPercentage,
        BigDecimal discountAmount,
        LocalDateTime endDate,
        Boolean isActive,
        String description
) {
}
