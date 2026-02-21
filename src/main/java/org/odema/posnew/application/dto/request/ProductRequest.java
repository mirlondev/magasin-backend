package org.odema.posnew.application.dto.request;

import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        String name,
        String description,
        BigDecimal price,
        UUID categoryId,
        String imageUrl,
        String sku,
        String barcode
) {
}

