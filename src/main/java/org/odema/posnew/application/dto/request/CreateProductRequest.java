package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProductRequest(
        @NotBlank @Size(max = 200) String name,
        String description,
        @NotNull UUID categoryId,
        String imageUrl,
        @Size(max = 50) String sku,
        @Size(max = 50) String barcode
) {
}
