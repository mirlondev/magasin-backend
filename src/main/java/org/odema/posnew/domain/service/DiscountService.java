package org.odema.posnew.domain.service;

import org.odema.posnew.application.dto.request.ApplyDiscountRequest;
import org.odema.posnew.application.dto.request.DiscountResult;

import java.math.BigDecimal;
import java.util.UUID;

public interface DiscountService {

    DiscountResult calculateItemDiscount(UUID productId, UUID storeId, Integer quantity, UUID customerId);


    DiscountResult applyDiscount(ApplyDiscountRequest request);

    BigDecimal calculateGlobalDiscount(UUID orderId, BigDecimal subtotal, UUID customerId);

    void validateDiscountRules(UUID storeId);
}
