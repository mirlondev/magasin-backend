package org.odema.posnew.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductResponse(
        UUID productId,
        String name,
        String description,
        BigDecimal price,
        Integer quantity,
        UUID categoryId,
        String categoryName,
        String imageUrl,

        String Filename,
        String sku,
        String barcode,
        Boolean inStock,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UUID storeId,
        Boolean isActive ,
        String storeName
) {}