package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        String description,
        String imageUrl,
        UUID parentCategoryId
) {
}
