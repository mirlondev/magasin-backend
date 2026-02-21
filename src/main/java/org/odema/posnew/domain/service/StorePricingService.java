package org.odema.posnew.domain.service;

import org.odema.posnew.application.dto.request.*;
import org.odema.posnew.application.dto.response.StoreProductPriceResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface StorePricingService {

    StoreProductPriceResponse setProductPrice(StoreProductPriceRequest request);

    StoreProductPriceResponse updatePrice(UUID priceId, UpdatePriceRequest request);

    void deactivatePrice(UUID priceId);

    StoreProductPriceResponse schedulePriceChange(SchedulePriceRequest request);

    StoreProductPriceResponse applyTemporaryDiscount(TemporaryDiscountRequest request);

    BigDecimal calculateFinalPrice(UUID productId, UUID storeId, UUID customerId);

    List<StoreProductPriceResponse> getActivePricesForStore(UUID storeId);

    List<StoreProductPriceResponse> getPriceHistory(UUID productId, UUID storeId);

    void bulkUpdatePrices(BulkPriceUpdateRequest request);
}
