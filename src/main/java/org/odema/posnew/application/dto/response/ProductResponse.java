package org.odema.posnew.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID productId,
        String name,
        String description,
        BigDecimal price,
        Integer quantity, // Calculated from Inventory

        UUID categoryId,
        String categoryName,

        String imageUrl,
        String imageFilename,
        String sku,
        String barcode,
        Boolean inStock,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        UUID storeId,    // Optional - for store-specific view
        Boolean isActive,

        String storeName // Optional - for store-specific view
) {
}
