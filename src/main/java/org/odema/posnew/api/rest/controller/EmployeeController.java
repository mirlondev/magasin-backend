package org.odema.posnew.api.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.application.dto.EmployeeResponse;
import org.odema.posnew.application.dto.response.ApiResponse;


import org.odema.posnew.domain.model.enums.UserRole;
import org.odema.posnew.domain.service.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


import org.odema.posnew.application.dto.request.EmployeeRequest;
import org.odema.posnew.application.dto.request.EmployeeUpdateRequest;


@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Tag(name = "Employees", description = "API de gestion des employés")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Créer un employé de boutique")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createStoreEmployee(
            @PathVariable UUID storeId,
            @Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.createStoreEmployee(request, storeId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employé de boutique créé avec succès", response));
    }

    @PostMapping("/warehouse/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "Créer un employé de dépôt")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createWarehouseEmployee(
            @PathVariable UUID storeId,
            @Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = employeeService.createWarehouseEmployee(request, storeId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Employé de dépôt créé avec succès", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir tous les employés (avec filtres optionnels)")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> findAllEmployees(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UserRole role) {
        List<EmployeeResponse> responses = employeeService.findAllEmployees(storeId, role);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir un employé par son ID")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployee(
            @PathVariable UUID employeeId) {
        EmployeeResponse response = employeeService.getEmployeeById(employeeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/store/{storeId}/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les employés par rôle dans un store")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getEmployeesByRoleInStore(
            @PathVariable UUID storeId,
            @PathVariable UserRole role) {
        List<EmployeeResponse> responses = employeeService.getEmployeesByRoleInStore(storeId, role);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/store/{storeId}/managers")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les managers d'un store")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getStoreManagers(
            @PathVariable UUID storeId) {
        List<EmployeeResponse> responses = employeeService.getStoreManagers(storeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/store/{storeId}/cashiers")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les caissiers d'un store")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getStoreCashiers(
            @PathVariable UUID storeId) {
        List<EmployeeResponse> responses = employeeService.getStoreCashiers(storeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Mettre à jour un employé")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable UUID employeeId,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        EmployeeResponse response = employeeService.updateEmployee(employeeId, request);
        return ResponseEntity.ok(ApiResponse.success("Employé mis à jour", response));
    }

    @PatchMapping("/{employeeId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Désactiver un employé")
    public ResponseEntity<ApiResponse<Void>> deactivateEmployee(
            @PathVariable UUID employeeId) {
        employeeService.deactivateEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employé désactivé", null));
    }

    @PatchMapping("/{employeeId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Réactiver un employé")
    public ResponseEntity<ApiResponse<Void>> activateEmployee(
            @PathVariable UUID employeeId) {
        employeeService.activateEmployee(employeeId);
        return ResponseEntity.ok(ApiResponse.success("Employé réactivé", null));
    }

    @PatchMapping("/{employeeId}/transfer/{newStoreId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Transférer un employé vers un autre store")
    public ResponseEntity<ApiResponse<EmployeeResponse>> transferEmployee(
            @PathVariable UUID employeeId,
            @PathVariable UUID newStoreId) {
        EmployeeResponse response = employeeService.transferEmployee(employeeId, newStoreId);
        return ResponseEntity.ok(ApiResponse.success("Employé transféré", response));
    }

    @PatchMapping("/{employeeId}/role/{newRole}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Changer le rôle d'un employé")
    public ResponseEntity<ApiResponse<EmployeeResponse>> changeEmployeeRole(
            @PathVariable UUID employeeId,
            @PathVariable UserRole newRole) {
        EmployeeResponse response = employeeService.changeEmployeeRole(employeeId, newRole);
        return ResponseEntity.ok(ApiResponse.success("Rôle de l'employé modifié", response));
    }
}
