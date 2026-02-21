package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.request.ProductRequest;
import org.odema.posnew.application.dto.response.ProductResponse;
import org.odema.posnew.domain.model.Category;
import org.odema.posnew.domain.model.Product;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request, Category category) {
        if (request == null) return null;

        return Product.builder()
                .name(request.name())
                .description(request.description())
                .category(category)
                .imageUrl(request.imageUrl())
                .sku(request.sku())
                .barcode(request.barcode())
                .isActive(true)
                .build();
    }

    public ProductResponse toResponse(Product product) {
        if (product == null) return null;

        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getStorePrices() != null && !product.getStorePrices().isEmpty()
                        ? product.getStorePrices().get(0).getFinalPrice() : null,
                product.getTotalStock(),
                product.getCategory() != null ? product.getCategory().getCategoryId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getImageUrl(),
                product.getImageFilename(),
                product.getSku(),
                product.getBarcode(),
                product.isInStock(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                null, // storeId
                product.getIsActive(),
                null  // storeName
        );
    }

    public ProductResponse toResponseWithStore(Product product, UUID storeId) {
        if (product == null) return null;

        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getEffectivePrice(storeId),
                product.getStockInStore(storeId),
                product.getCategory() != null ? product.getCategory().getCategoryId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getFullImageUrl(null),
                product.getImageFilename(),
                product.getSku(),
                product.getBarcode(),
                product.getStockInStore(storeId) > 0,
                product.getCreatedAt(),
                product.getUpdatedAt(),
                storeId,
                product.getIsActive(),
                null  // storeName - would need to be fetched separately
        );
    }

    public List<ProductResponse> toResponseList(List<Product> products) {
        if (products == null) return List.of();
        return products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}