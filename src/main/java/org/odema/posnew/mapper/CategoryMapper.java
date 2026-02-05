package org.odema.posnew.mapper;


import org.odema.posnew.dto.request.CategoryRequest;
import org.odema.posnew.dto.response.CategoryResponse;
import org.odema.posnew.entity.Category;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request) {
        if (request == null) return null;

        return Category.builder()
                .name(request.name())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .build();
    }

    public CategoryResponse toResponse(Category category) {
        if (category == null) return null;

        List<CategoryResponse> subCategories = category.getSubCategories() != null
                ? category.getSubCategories().stream()
                .map(this::toResponse)
                .collect(Collectors.toList())
                : Collections.emptyList();

        return new CategoryResponse(
                category.getCategoryId(),
                category.getName(),
                category.getDescription(),
                category.getImageUrl(),
                category.getParentCategory() != null ? category.getParentCategory().getCategoryId() : null,
                category.getParentCategory() != null ? category.getParentCategory().getName() : null,
                subCategories,
                category.getProducts() != null ? category.getProducts().size() : 0,
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getIsActive()
        );
    }
}