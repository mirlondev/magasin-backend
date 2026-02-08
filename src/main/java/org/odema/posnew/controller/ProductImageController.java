package org.odema.posnew.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.response.ApiResponse;
import org.odema.posnew.dto.response.ProductImageResponse;
import org.odema.posnew.service.ProductImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/products/{productId}/image")
@RequiredArgsConstructor
@Tag(name = "Product Images", description = "API de gestion des images des produits")
@SecurityRequirement(name = "bearerAuth")
public class ProductImageController {

    private final ProductImageService productImageService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Uploader une image pour un produit")
    public ResponseEntity<ApiResponse<ProductImageResponse>> uploadProductImage(
            @PathVariable UUID productId,
            @RequestParam("image") MultipartFile imageFile) {

        try {
            if (!productImageService.validateProductImage(imageFile)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Format d'image non supporté. Formats acceptés: JPG, JPEG, PNG, GIF, WEBP. Taille max: 5MB"));
            }

            String imageUrl = productImageService.uploadProductImage(productId, imageFile);

            ProductImageResponse response = new ProductImageResponse(
                    productId,
                    imageUrl,
                    imageFile.getOriginalFilename(),
                    imageFile.getSize(),
                    imageFile.getContentType()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Image uploadée avec succès", response));

        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de l'upload de l'image: " + e.getMessage()));
        }
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Supprimer l'image d'un produit")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(@PathVariable UUID productId) {
        try {
            productImageService.deleteProductImage(productId);
            return ResponseEntity.ok(ApiResponse.success("Image supprimée avec succès", null));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la suppression de l'image: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Obtenir l'URL de l'image d'un produit")
    public ResponseEntity<ApiResponse<ProductImageResponse>> getProductImage(@PathVariable UUID productId) {
        try {
            String imageUrl = productImageService.getProductImageUrl(productId);

            if (imageUrl == null) {
                return ResponseEntity.ok(ApiResponse.success("Ce produit n'a pas d'image", null));
            }

            ProductImageResponse response = new ProductImageResponse(
                    productId,
                    imageUrl,
                    null, // filename
                    null, // size
                    null  // contentType
            );

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Mettre à jour l'image d'un produit")
    public ResponseEntity<ApiResponse<ProductImageResponse>> updateProductImage(
            @PathVariable UUID productId,
            @RequestParam("image") MultipartFile imageFile) {

        try {
            if (!productImageService.validateProductImage(imageFile)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Format d'image non supporté. Formats acceptés: JPG, JPEG, PNG, GIF, WEBP. Taille max: 5MB"));
            }

            String imageUrl = productImageService.updateProductImage(productId, imageFile);

            ProductImageResponse response = new ProductImageResponse(
                    productId,
                    imageUrl,
                    imageFile.getOriginalFilename(),
                    imageFile.getSize(),
                    imageFile.getContentType()
            );

            return ResponseEntity.ok(ApiResponse.success("Image mise à jour avec succès", response));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la mise à jour de l'image: " + e.getMessage()));
        }
    }
}
