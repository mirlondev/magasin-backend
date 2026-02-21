package org.odema.posnew.domain.service;

import org.odema.posnew.application.dto.request.SetProductPriceRequest;
import org.odema.posnew.application.dto.request.TemporaryDiscountRequest;
import org.odema.posnew.application.dto.request.UpdatePriceRequest;

import java.math.BigDecimal;

public interface StorePricingService {

    StoreProductPriceResponse setProductPrice(SetProductPriceRequest request);

    StoreProductPriceResponse updatePrice(UUID priceId, UpdatePriceRequest request);

    void deactivatePrice(UUID priceId);

    StoreProductPriceResponse schedulePriceChange(SchedulePriceRequest request);

    StoreProductPriceResponse applyTemporaryDiscount(TemporaryDiscountRequest request);

    BigDecimal calculateFinalPrice(UUID productId, UUID storeId, UUID customerId);

    List<StoreProductPriceResponse> getActivePricesForStore(UUID storeId);

    List<StoreProductPriceResponse> getPriceHistory(UUID productId, UUID storeId);

    void bulkUpdatePrices(BulkPriceUpdateRequest request);
}
