package org.odema.posnew.mapper;

import org.odema.posnew.dto.request.ProductRequest;
import org.odema.posnew.dto.response.ProductResponse;
import org.odema.posnew.entity.Category;
import org.odema.posnew.entity.Product;
import org.odema.posnew.entity.Store;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request, Category category, Store store) {
        if (request == null) return null;

        return Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .quantity(request.quantity())
                .category(category)
                .imageUrl(request.imageUrl())
                .sku(request.sku())
                .barcode(request.barcode())
                .build();
    }

    public ProductResponse toResponse(Product product) {
        if (product == null) return null;

        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getQuantity(),
                product.getCategory() != null ? product.getCategory().getCategoryId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getImageUrl(),
                product.getSku(),
                product.getBarcode(),
                product.isInStock(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                null, // storeId sera complété si nécessaire
                null  // storeName sera complété si nécessaire
        );
    }
}