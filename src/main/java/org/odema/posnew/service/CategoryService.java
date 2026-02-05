package org.odema.posnew.service;


import org.odema.posnew.dto.request.CategoryRequest;
import org.odema.posnew.dto.response.CategoryResponse;
import org.odema.posnew.exception.NotFoundException;

import java.util.List;
import java.util.UUID;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request) throws NotFoundException;
    CategoryResponse getCategoryById(UUID categoryId) throws NotFoundException;
    CategoryResponse updateCategory(UUID categoryId, CategoryRequest request) throws NotFoundException;
    void deleteCategory(UUID categoryId) throws NotFoundException;
    List<CategoryResponse> getAllCategories();
    List<CategoryResponse> getMainCategories();
    List<CategoryResponse> getSubCategories(UUID parentId);
    List<CategoryResponse> searchCategories(String keyword);
}