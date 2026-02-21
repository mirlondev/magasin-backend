package org.odema.posnew.api.rest.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.application.dto.request.StoreRequest;
import org.odema.posnew.application.dto.response.ApiResponse;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.api.exception.UnauthorizedException;
import org.odema.posnew.application.dto.response.StoreResponse;
import org.odema.posnew.domain.model.User;
import org.odema.posnew.domain.model.enums.StoreType;
import org.odema.posnew.domain.service.StoreService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
@Tag(name = "Stores", description = "API de gestion des stores (dépôts et boutiques)")
@SecurityRequirement(name = "bearerAuth")
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DEPOT_MANAGER')")
    @Operation(summary = "Créer un nouveau store")
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @Valid @RequestBody StoreRequest storeDto,
            @AuthenticationPrincipal UserDetails userDetails) throws UnauthorizedException {
        StoreResponse createdStore = storeService.createStore(storeDto, (User) userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Store créé avec succès", createdStore));
    }

    @GetMapping("/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir un store par son ID")
    public ResponseEntity<ApiResponse<StoreResponse>> getStore(@PathVariable UUID storeId) throws NotFoundException {
        StoreResponse store = storeService.findStoreById(storeId);
        return ResponseEntity.ok(ApiResponse.success(store));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir tous les stores")
    public ResponseEntity<ApiResponse<List<StoreResponse>>> getAllStores() {
        List<StoreResponse> stores = storeService.getAllStores();
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @GetMapping("/type/{storeType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les stores par type")
    public ResponseEntity<ApiResponse<List<StoreResponse>>> getStoresByType(
            @PathVariable StoreType storeType) {
        List<StoreResponse> stores = storeService.getStoresByType(storeType);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @PutMapping("/{storeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre à jour un store")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @PathVariable UUID storeId,
            @RequestBody @Valid StoreRequest storeDto) throws NotFoundException {
        StoreResponse updatedStore = storeService.updateStore(storeId, storeDto);
        return ResponseEntity.ok(ApiResponse.success("Store mis à jour", updatedStore));
    }

    @DeleteMapping("/{storeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un store")
    public ResponseEntity<ApiResponse<Void>> deleteStore(@PathVariable UUID storeId) throws NotFoundException {
        storeService.deleteStore(storeId);
        return ResponseEntity.ok(ApiResponse.success("Store supprimé", null));
    }
}