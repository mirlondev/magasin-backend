package org.odema.posnew.api.rest.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.application.dto.request.CashRegisterRequest;
import org.odema.posnew.application.dto.response.ApiResponse;
import org.odema.posnew.application.dto.response.CashRegisterResponse;
import org.odema.posnew.application.service.CashRegisterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cash-registers")
@RequiredArgsConstructor
@Tag(name = "Cash Registers", description = "API de gestion des caisses")
@SecurityRequirement(name = "bearerAuth")
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Créer une nouvelle caisse")
    public ResponseEntity<ApiResponse<CashRegisterResponse>> createCashRegister(
            @Valid @RequestBody CashRegisterRequest request) {
        CashRegisterResponse response = cashRegisterService.createCashRegister(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Caisse créée avec succès", response));
    }

    @GetMapping("/{cashRegisterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Obtenir une caisse par son ID")
    public ResponseEntity<ApiResponse<CashRegisterResponse>> getCashRegister(
            @PathVariable UUID cashRegisterId) {
        CashRegisterResponse response = cashRegisterService.getCashRegisterById(cashRegisterId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{registerNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Obtenir une caisse par son numéro")
    public ResponseEntity<ApiResponse<CashRegisterResponse>> getCashRegisterByNumber(
            @PathVariable String registerNumber) {
        CashRegisterResponse response = cashRegisterService.getCashRegisterByNumber(registerNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Obtenir toutes les caisses d'un store")
    public ResponseEntity<ApiResponse<List<CashRegisterResponse>>> getCashRegistersByStore(
            @PathVariable UUID storeId) {
        List<CashRegisterResponse> responses = cashRegisterService.getAllCashRegistersByStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/store/{storeId}/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Obtenir les caisses actives d'un store")
    public ResponseEntity<ApiResponse<List<CashRegisterResponse>>> getActiveCashRegistersByStore(
            @PathVariable UUID storeId) {
        List<CashRegisterResponse> responses = cashRegisterService.getActiveCashRegistersByStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{cashRegisterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Mettre à jour une caisse")
    public ResponseEntity<ApiResponse<CashRegisterResponse>> updateCashRegister(
            @PathVariable UUID cashRegisterId,
            @Valid @RequestBody CashRegisterRequest request) {
        CashRegisterResponse response = cashRegisterService.updateCashRegister(cashRegisterId, request);
        return ResponseEntity.ok(ApiResponse.success("Caisse mise à jour", response));
    }

    @DeleteMapping("/{cashRegisterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Supprimer une caisse")
    public ResponseEntity<ApiResponse<Void>> deleteCashRegister(
            @PathVariable UUID cashRegisterId) {
        cashRegisterService.deleteCashRegister(cashRegisterId);
        return ResponseEntity.ok(ApiResponse.success("Caisse supprimée", null));
    }

    @PatchMapping("/{cashRegisterId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Activer une caisse")
    public ResponseEntity<ApiResponse<Void>> activateCashRegister(
            @PathVariable UUID cashRegisterId) {
        cashRegisterService.activateCashRegister(cashRegisterId);
        return ResponseEntity.ok(ApiResponse.success("Caisse activée", null));
    }

    @PatchMapping("/{cashRegisterId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Désactiver une caisse")
    public ResponseEntity<ApiResponse<Void>> deactivateCashRegister(
            @PathVariable UUID cashRegisterId) {
        cashRegisterService.deactivateCashRegister(cashRegisterId);
        return ResponseEntity.ok(ApiResponse.success("Caisse désactivée", null));
    }
}