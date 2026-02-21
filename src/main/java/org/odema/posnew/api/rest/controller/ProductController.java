package org.odema.posnew.api.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.application.dto.request.ProductRequest;
import org.odema.posnew.application.dto.response.ApiResponse;
import org.odema.posnew.application.dto.response.PaginatedResponse;
import org.odema.posnew.application.dto.response.ProductResponse;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.api.exception.UnauthorizedException;
import org.odema.posnew.domain.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "API de gestion des produits")
@SecurityRequirement(name = "bearerAuth")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Créer un nouveau produit")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request,
            @AuthenticationPrincipal UserDetails userDetails) throws UnauthorizedException, NotFoundException {
        // Convertir UserDetails en User entity si nécessaire
        // Pour simplifier, on suppose que userDetails contient les informations nécessaires
        ProductResponse response = productService.createProduct(request, null); // À adapter
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Produit créé avec succès", response));
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Obtenir un produit par son ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProduct(
            @PathVariable UUID productId) throws NotFoundException {
        ProductResponse response = productService.getProductById(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Obtenir tous les produits")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getAllProducts (@PageableDefault(sort = "name", direction = Sort.Direction.ASC)  Pageable pageable) {

        if (pageable.getSort().isSorted()) {
            pageable.getSort().forEach(order -> {
                if (!List.of("name", "sku", "barcode", "createdAt").contains(order.getProperty())) {
                    throw new BadRequestException("Champ de tri invalide : " + order.getProperty());
                }
            });
        }
        Page<ProductResponse> responses = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(responses)));

    }

    @GetMapping("/category/{categoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Obtenir les produits par catégorie")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByCategory(
            @PathVariable UUID categoryId) {
        List<ProductResponse> responses = productService.getProductsByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Rechercher des produits")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam String keyword) {
        List<ProductResponse> responses = productService.searchProducts(keyword);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les produits en stock faible")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStockProducts(
            @RequestParam(required = false, defaultValue = "10") Integer threshold) {
        List<ProductResponse> responses = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Mettre à jour un produit")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest request) throws NotFoundException {
        ProductResponse response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Produit mis à jour", response));
    }

    @PatchMapping("/{productId}/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Mettre à jour le stock d'un produit")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable UUID productId,
            @RequestParam Integer quantity,
            @RequestParam(defaultValue = "set") String operation) throws NotFoundException {
        ProductResponse response = productService.updateStock(productId, quantity, operation);
        return ResponseEntity.ok(ApiResponse.success("Stock mis à jour", response));
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "Supprimer un produit")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID productId) throws NotFoundException {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Produit supprimé", null));
    }
}