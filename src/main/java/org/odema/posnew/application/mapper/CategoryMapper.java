package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.CategoryRequest;
import org.odema.posnew.application.dto.CategoryResponse;
import org.odema.posnew.domain.model.Category;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request, Category parentCategory) {
        if (request == null) return null;

        return Category.builder()
                .name(request.name())
                .description(request.description())
                .imageUrl(request.imageUrl())
                .parentCategory(parentCategory)
                .isActive(true)
                .build();
    }

    public CategoryResponse toResponse(Category category) {
        if (category == null) return null;

        return new CategoryResponse(
                category.getCategoryId(),
                category.getName(),
                category.getDescription(),
                category.getImageUrl(),
                category.getParentCategory() != null ? category.getParentCategory().getCategoryId() : null,
                category.getParentCategory() != null ? category.getParentCategory().getName() : null,
                mapSubCategories(category.getSubCategories()),
                category.getProducts() != null ? category.getProducts().size() : 0,
                category.getCreatedAt(),
                category.getUpdatedAt(),
                category.getIsActive()
        );
    }

    public List<CategoryResponse> toResponseList(List<Category> categories) {
        if (categories == null) return List.of();
        return categories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private List<CategoryResponse> mapSubCategories(List<Category> subCategories) {
        if (subCategories == null) return List.of();
        return subCategories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}