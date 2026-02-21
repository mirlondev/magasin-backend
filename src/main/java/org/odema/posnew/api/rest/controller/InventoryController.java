package org.odema.posnew.api.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.odema.posnew.application.dto.*;
import org.odema.posnew.application.dto.InventoryRequest;
import org.odema.posnew.application.dto.InventoryResponse;
import org.odema.posnew.application.dto.InventorySummaryResponse;
import org.odema.posnew.application.dto.InventoryTransferRequest;
import org.odema.posnew.application.dto.InventoryUpdateRequest;
import org.odema.posnew.application.dto.response.ApiResponse;
import org.odema.posnew.application.dto.response.PaginatedResponse;
import org.odema.posnew.domain.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "API de gestion des stocks")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Créer un nouvel inventaire")
    public ResponseEntity<ApiResponse<InventoryResponse>> createInventory(
            @Valid @RequestBody InventoryRequest request) {
        InventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inventaire créé avec succès", response));
    }

    @GetMapping("/{inventoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir un inventaire par son ID")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventory(
            @PathVariable UUID inventoryId) {
        InventoryResponse response = inventoryService.getInventoryById(inventoryId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/product/{productId}/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir l'inventaire d'un produit dans un store spécifique")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByProductAndStore(
            @PathVariable UUID productId,
            @PathVariable UUID storeId) {
        InventoryResponse response = inventoryService.getInventoryByProductAndStore(productId, storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Méthode paginée principale avec filtrage
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir tous les inventaires (paginé)")
    public ResponseEntity<ApiResponse<PaginatedResponse<InventoryResponse>>> getInventoryPaginated(
            @RequestParam(required = false) UUID storeId,
            Pageable pageable) {
        Page<InventoryResponse> page = inventoryService.getInventory(storeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(page)));
    }

    // Méthode non paginée pour compatibilité
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir tous les inventaires (liste complète)")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllInventory() {
        List<InventoryResponse> responses = inventoryService.getAllInventory();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // Méthode paginée par store
    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les inventaires d'un store (paginé)")
    public ResponseEntity<ApiResponse<PaginatedResponse<InventoryResponse>>> getInventoryByStorePaginated(
            @PathVariable UUID storeId,
            Pageable pageable) {
        Page<InventoryResponse> page = inventoryService.getInventoryByStore(storeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(page)));
    }

    // Méthode non paginée par store pour compatibilité
    @GetMapping("/store/{storeId}/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les inventaires d'un store (liste complète)")
    public ResponseEntity<ApiResponse<Page<InventoryResponse>>> getInventoryByStore(
            @PathVariable UUID storeId, Pageable pageable) {
        Page<InventoryResponse> responses = inventoryService.getInventoryByStore(storeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // Méthode paginée par produit
    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les inventaires d'un produit (paginé)")
    public ResponseEntity<ApiResponse<PaginatedResponse<InventoryResponse>>> getInventoryByProductPaginated(
            @PathVariable UUID productId,
            Pageable pageable) {
        Page<InventoryResponse> page = inventoryService.getInventoryByProduct(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(page)));
    }

    // Méthode non paginée par produit pour compatibilité
    @GetMapping("/product/{productId}/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les inventaires d'un produit (liste complète)")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventoryByProduct(
            @PathVariable UUID productId) {
        List<InventoryResponse> responses = inventoryService.getInventoryByProduct(productId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // Méthode paginée pour stock faible
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les inventaires en stock faible (paginé)")
    public ResponseEntity<ApiResponse<PaginatedResponse<InventoryResponse>>> getLowStockInventoryPaginated(
            @RequestParam(required = false, defaultValue = "10") Integer threshold,
            Pageable pageable) {
        Page<InventoryResponse> page = inventoryService.getLowStockInventory(threshold, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(page)));
    }

    // Méthode non paginée pour stock faible pour compatibilité
    @GetMapping("/low-stock/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les inventaires en stock faible (liste complète)")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStockInventory(
            @RequestParam(required = false, defaultValue = "10") Integer threshold) {
        List<InventoryResponse> responses = inventoryService.getLowStockInventory(threshold);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // Méthode paginée par statut
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les inventaires par statut (paginé)")
    public ResponseEntity<ApiResponse<PaginatedResponse<InventoryResponse>>> getInventoryByStatusPaginated(
            @PathVariable String status,
            Pageable pageable) {
        Page<InventoryResponse> page = inventoryService.getInventoryByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(page)));
    }

    // Méthode non paginée par statut pour compatibilité
    @GetMapping("/status/{status}/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les inventaires par statut (liste complète)")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventoryByStatus(
            @PathVariable String status) {
        List<InventoryResponse> responses = inventoryService.getInventoryByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{inventoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Mettre à jour un inventaire")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @PathVariable UUID inventoryId,
            @Valid @RequestBody InventoryUpdateRequest request) {
        InventoryResponse response = inventoryService.updateInventory(inventoryId, request);
        return ResponseEntity.ok(ApiResponse.success("Inventaire mis à jour", response));
    }

    @PatchMapping("/{inventoryId}/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Mettre à jour le stock d'un inventaire")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateStock(
            @PathVariable UUID inventoryId,
            @RequestParam Integer quantity,
            @RequestParam(defaultValue = "set") String operation) {
        InventoryResponse response = inventoryService.updateStock(inventoryId, quantity, operation);
        return ResponseEntity.ok(ApiResponse.success("Stock mis à jour", response));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Transférer du stock entre stores")
    public ResponseEntity<ApiResponse<InventoryResponse>> transferStock(
            @Valid @RequestBody InventoryTransferRequest request) {
        InventoryResponse response = inventoryService.transferStock(request);
        return ResponseEntity.ok(ApiResponse.success("Transfert effectué", response));
    }

    @GetMapping("/store/{storeId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir le résumé de l'inventaire d'un store")
    public ResponseEntity<ApiResponse<org.odema.posnew.application.dto.InventorySummaryResponse>> getInventorySummary(
            @PathVariable UUID storeId) {
        InventorySummaryResponse response = inventoryService.getInventorySummary(storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/store/{storeId}/value")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir la valeur totale du stock d'un store")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalStockValue(
            @PathVariable UUID storeId) {
        BigDecimal value = inventoryService.getTotalStockValue(storeId);
        return ResponseEntity.ok(ApiResponse.success(value));
    }

    @PostMapping("/{inventoryId}/restock")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Réapprovisionner un inventaire")
    public ResponseEntity<ApiResponse<Void>> restockInventory(
            @PathVariable UUID inventoryId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) BigDecimal unitCost) {
        inventoryService.restockInventory(inventoryId, quantity, unitCost);
        return ResponseEntity.ok(ApiResponse.success("Réapprovisionnement effectué", null));
    }

    @DeleteMapping("/{inventoryId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "Supprimer un inventaire")
    public ResponseEntity<ApiResponse<Void>> deleteInventory(@PathVariable UUID inventoryId) {
        inventoryService.deleteInventory(inventoryId);
        return ResponseEntity.ok(ApiResponse.success("Inventaire supprimé", null));
    }
}