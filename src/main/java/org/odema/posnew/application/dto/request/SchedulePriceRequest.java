package org.odema.posnew.application.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SchedulePriceRequest(
        UUID productId,
        UUID storeId,
        BigDecimal newBasePrice,
        BigDecimal taxRate,
        BigDecimal discountPercentage,
        BigDecimal discountAmount,
        LocalDateTime effectiveDate,
        LocalDateTime endDate


) {

}
