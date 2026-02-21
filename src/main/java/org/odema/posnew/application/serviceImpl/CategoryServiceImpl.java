package org.odema.posnew.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.application.dto.request.CategoryRequest;
import org.odema.posnew.application.dto.response.CategoryResponse;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.application.mapper.CategoryMapper;
import org.odema.posnew.repository.CategoryRepository;
import org.odema.posnew.application.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) throws NotFoundException {
        // Vérifier l'unicité du nom
        if (request.parentCategoryId() == null) {
            if (categoryRepository.existsByNameAndParentCategoryIsNull(request.name())) {
                throw new BadRequestException("Une catégorie principale avec ce nom existe déjà");
            }
        } else {
            if (categoryRepository.existsByNameAndParentCategory_CategoryId(
                    request.name(), request.parentCategoryId())) {
                throw new BadRequestException("Une sous-catégorie avec ce nom existe déjà dans cette catégorie");
            }
        }

        // Récupérer la catégorie parent si spécifiée
        Category parentCategory = null;
        if (request.parentCategoryId() != null) {
            parentCategory = categoryRepository.findById(request.parentCategoryId())
                    .orElseThrow(() -> new NotFoundException("Catégorie parent non trouvée"));
        }

        // Créer la catégorie
        Category category = categoryMapper.toEntity(request);
        category.setParentCategory(parentCategory);
        category.setIsActive(true);

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    public CategoryResponse getCategoryById(UUID categoryId) throws NotFoundException {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Catégorie non trouvée"));

        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, CategoryRequest request) throws NotFoundException {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Catégorie non trouvée"));

        // Mettre à jour les champs
        if (request.name() != null) category.setName(request.name());
        if (request.description() != null) category.setDescription(request.description());
        if (request.imageUrl() != null) category.setImageUrl(request.imageUrl());

        // Mettre à jour le parent si spécifié
        if (request.parentCategoryId() != null) {
            if (request.parentCategoryId().equals(categoryId)) {
                throw new BadRequestException("Une catégorie ne peut pas être son propre parent");
            }

            Category parentCategory = categoryRepository.findById(request.parentCategoryId())
                    .orElseThrow(() -> new NotFoundException("Catégorie parent non trouvée"));
            category.setParentCategory(parentCategory);
        } else if (category.getParentCategory() != null) {
            category.setParentCategory(null); // Devenir une catégorie principale
        }

        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID categoryId) throws NotFoundException {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Catégorie non trouvée"));

        // Vérifier si la catégorie a des produits
        if (!category.getProducts().isEmpty()) {
            throw new BadRequestException(
                    "Impossible de supprimer une catégorie contenant des produits. " +
                            "Veuillez d'abord déplacer ou supprimer les produits."
            );
        }

        // Transférer les sous-catégories au parent ou les rendre principales
        for (Category subCategory : category.getSubCategories()) {
            if (category.getParentCategory() != null) {
                subCategory.setParentCategory(category.getParentCategory());
            } else {
                subCategory.setParentCategory(null);
            }
            categoryRepository.save(subCategory);
        }

        categoryRepository.delete(category);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllActiveCategories().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> getMainCategories() {
        return categoryRepository.findByParentCategoryIsNull().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> getSubCategories(UUID parentId) {
        return categoryRepository.findByParentCategory_CategoryId(parentId).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> searchCategories(String keyword) {
        return categoryRepository.searchByName(keyword).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }
}