package org.odema.posnew.mapper;

import org.odema.posnew.dto.request.ProductRequest;
import org.odema.posnew.dto.response.ProductResponse;
import org.odema.posnew.entity.Category;
import org.odema.posnew.entity.Product;
import org.odema.posnew.entity.Store;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductMapper {

    /**
     * Convert ProductRequest to Product entity.
     * Note: Quantity is NOT set here - it comes from Inventory!
     */
    public Product toEntity(ProductRequest request, Category category, Store store) {
        if (request == null) return null;

        return Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
              //  .quantity(0) // Always 0 - actual stock tracked in Inventory
                .category(category)
                .imageUrl(request.imageUrl())
                .sku(request.sku())
                .barcode(request.barcode())
                .build();
    }

    /**
     * Convert Product entity to ProductResponse.
     * Uses getTotalStock() to calculate quantity from Inventory.
     */
    public ProductResponse toResponse(Product product) {
        if (product == null) return null;

        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getTotalStock(), // ← CALCULATED from Inventory!
                product.getCategory() != null ? product.getCategory().getCategoryId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getImageUrl(),
                product.getImageFilename(),
                product.getSku(),
                product.getBarcode(),
                product.isInStock(), // ← Also calculated
                product.getCreatedAt(),
                product.getUpdatedAt(),
                null, // storeId - not applicable for product
                product.getIsActive(),

                null  // storeName - not applicable for product
        );
    }

    /**
     * Convert Product to ProductResponse with store-specific quantity.
     * Useful when showing product availability in a specific store.
     */
    public ProductResponse toResponseWithStore(Product product, UUID storeId) {
        if (product == null) return null;

        Integer storeQuantity = product.getStockInStore(storeId);

        return new ProductResponse(
                product.getProductId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                storeQuantity, // ← Store-specific quantity
                product.getCategory() != null ? product.getCategory().getCategoryId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getFullImageUrl(), // Retourne l'URL complète de l'image
                product.getImageFilename(),
                product.getSku(),
                product.getBarcode(),
                storeQuantity > 0, // In stock in THIS store
                product.getCreatedAt(),
                product.getUpdatedAt(),
                storeId,
                product.getIsActive(),

                null // Store name would need to be fetched

        );
    }
}