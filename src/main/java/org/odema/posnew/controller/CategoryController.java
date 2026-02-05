package org.odema.posnew.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.CategoryRequest;
import org.odema.posnew.dto.response.ApiResponse;
import org.odema.posnew.dto.response.CategoryResponse;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "API de gestion des catégories")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Créer une nouvelle catégorie")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) throws NotFoundException {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Catégorie créée avec succès", response));
    }

    @GetMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Obtenir une catégorie par son ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(
            @PathVariable UUID categoryId) throws NotFoundException {
        CategoryResponse response = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Obtenir toutes les catégories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> responses = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/main")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Obtenir les catégories principales")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getMainCategories() {
        List<CategoryResponse> responses = categoryService.getMainCategories();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{parentId}/subcategories")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Obtenir les sous-catégories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getSubCategories(
            @PathVariable UUID parentId) {
        List<CategoryResponse> responses = categoryService.getSubCategories(parentId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Rechercher des catégories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> searchCategories(
            @RequestParam String keyword) {
        List<CategoryResponse> responses = categoryService.searchCategories(keyword);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Mettre à jour une catégorie")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody CategoryRequest request) throws NotFoundException {
        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(ApiResponse.success("Catégorie mise à jour", response));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "Supprimer une catégorie")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID categoryId) throws NotFoundException {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Catégorie supprimée", null));
    }
}