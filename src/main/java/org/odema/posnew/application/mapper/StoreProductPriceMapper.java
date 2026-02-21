package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.request.SetProductPriceRequest;
import org.odema.posnew.application.dto.response.StoreProductPriceResponse;
import org.odema.posnew.domain.model.StoreProductPrice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StoreProductPriceMapper {

    public StoreProductPrice toEntity(SetProductPriceRequest request) {
        if (request == null) return null;

        return StoreProductPrice.builder()
                .basePrice(request.newBasePrice())
                .taxRate(request.taxRate())
                .discountPercentage(request.discountPercentage())
                .discountAmount(request.discountAmount())
                .effectiveDate(request.effectiveDate())
                .endDate(request.endDate())
                .description(request.description())
                .isActive(true)
                .build();
    }

    public StoreProductPriceResponse toResponse(StoreProductPrice price) {
        if (price == null) return null;

        return new StoreProductPriceResponse(
                price.getPriceId(),
                price.getProduct() != null ? price.getProduct().getProductId() : null,
                price.getProduct() != null ? price.getProduct().getName() : null,
                price.getStore() != null ? price.getStore().getStoreId() : null,
                price.getStore() != null ? price.getStore().getName() : null,
                price.getBasePrice(),
                price.getTaxRate(),
                price.getTaxAmount(),
                price.getDiscountPercentage(),
                price.getDiscountValue(),
                price.getFinalPrice(),
                price.getEffectiveDate(),
                price.getEndDate(),
                price.getIsActive(),
                price.hasActiveDiscount(),
                price.getCreatedAt()
        );
    }

    public List<StoreProductPriceResponse> toResponseList(List<StoreProductPrice> prices) {
        if (prices == null) return List.of();
        return prices.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void updateEntityFromRequest(StoreProductPrice price, SetProductPriceRequest request) {
        if (request == null || price == null) return;

        if (request.newBasePrice() != null) price.setBasePrice(request.newBasePrice());
        if (request.taxRate() != null) price.setTaxRate(request.taxRate());
        if (request.discountPercentage() != null) price.setDiscountPercentage(request.discountPercentage());
        if (request.discountAmount() != null) price.setDiscountAmount(request.discountAmount());
        if (request.effectiveDate() != null) price.setEffectiveDate(request.effectiveDate());
        if (request.endDate() != null) price.setEndDate(request.endDate());
        if (request.description() != null) price.setDescription(request.description());
    }
}