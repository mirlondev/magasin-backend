package org.odema.posnew.application.dto.request;

import java.util.UUID;

public record UpdateProductRequest(
        String name,
        String description,
        UUID categoryId,
        String imageUrl,
        String sku,
        String barcode,
        Boolean isActive
) {
}
