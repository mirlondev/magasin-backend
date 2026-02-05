package org.odema.posnew.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.StoreDto;
import org.odema.posnew.dto.response.ApiResponse;
import org.odema.posnew.entity.User;
import org.odema.posnew.entity.enums.StoreType;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.exception.UnauthorizedException;
import org.odema.posnew.service.StoreService;
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
    public ResponseEntity<ApiResponse<StoreDto>> createStore(
            @Valid @RequestBody StoreDto storeDto,
            @AuthenticationPrincipal UserDetails userDetails) throws UnauthorizedException {
        StoreDto createdStore = storeService.createStore(storeDto, (User) userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Store créé avec succès", createdStore));
    }

    @GetMapping("/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir un store par son ID")
    public ResponseEntity<ApiResponse<StoreDto>> getStore(@PathVariable UUID storeId) throws NotFoundException {
        StoreDto store = storeService.findStoreById(storeId);
        return ResponseEntity.ok(ApiResponse.success(store));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir tous les stores")
    public ResponseEntity<ApiResponse<List<StoreDto>>> getAllStores() {
        List<StoreDto> stores = storeService.getAllStores();
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @GetMapping("/type/{storeType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les stores par type")
    public ResponseEntity<ApiResponse<List<StoreDto>>> getStoresByType(
            @PathVariable StoreType storeType) {
        List<StoreDto> stores = storeService.getStoresByType(storeType);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @PutMapping("/{storeId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mettre à jour un store")
    public ResponseEntity<ApiResponse<StoreDto>> updateStore(
            @PathVariable UUID storeId,
            @Valid @RequestBody StoreDto storeDto) throws NotFoundException {
        StoreDto updatedStore = storeService.updateStore(storeId, storeDto);
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