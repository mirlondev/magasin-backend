package org.odema.posnew.application.dto.request;

import org.odema.posnew.domain.model.enums.OrderType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderRequest(
        UUID customerId,
        UUID storeId,
        List<OrderItemRequest> items,
        OrderType orderType,
        BigDecimal discountAmount,
        BigDecimal taxRate,
        Boolean isTaxable,
        String notes,
        BigDecimal globalDiscountPercentage,
        BigDecimal globalDiscountAmount
) {
}
