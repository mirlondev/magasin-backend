package org.odema.posnew.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StoreProductPriceResponse(
        UUID priceId,
        UUID productId,
        String productName,
        UUID storeId,
        String storeName,
        BigDecimal basePrice,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal discountPercentage,
        BigDecimal discountAmount,
        BigDecimal finalPrice,
        LocalDateTime effectiveDate,
        LocalDateTime endDate,
        boolean isActive,
        boolean hasActiveDiscount,
        LocalDateTime createdAt
) {
}
