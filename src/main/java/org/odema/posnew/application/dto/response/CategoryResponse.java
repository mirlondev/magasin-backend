package org.odema.posnew.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CategoryResponse(
        UUID categoryId,
        String name,
        String description,
        String imageUrl,
        UUID parentCategoryId,
        String parentCategoryName,
        List<CategoryResponse> subCategories,
        Integer productCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isActive
) {
}
