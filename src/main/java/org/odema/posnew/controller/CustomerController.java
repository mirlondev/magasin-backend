package org.odema.posnew.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.CustomerRequest;
import org.odema.posnew.dto.response.ApiResponse;
import org.odema.posnew.dto.response.CustomerResponse;
import org.odema.posnew.service.CustomerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "API de gestion des clients")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Créer un nouveau client")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Client créé avec succès", response));
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir un client par son ID")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
            @PathVariable UUID customerId) {
        CustomerResponse response = customerService.getCustomerById(customerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir tous les clients")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers() {
        List<CustomerResponse> responses = customerService.getAllCustomers();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Rechercher des clients")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> searchCustomers(
            @RequestParam String keyword) {
        List<CustomerResponse> responses = customerService.searchCustomers(keyword);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/top/{limit}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les meilleurs clients")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getTopCustomers(
            @PathVariable int limit) {
        List<CustomerResponse> responses = customerService.getTopCustomers(limit);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Mettre à jour un client")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.updateCustomer(customerId, request);
        return ResponseEntity.ok(ApiResponse.success("Client mis à jour", response));
    }

    @PatchMapping("/{customerId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Désactiver un client")
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(
            @PathVariable UUID customerId) {
        customerService.deactivateCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success("Client désactivé", null));
    }

    @PatchMapping("/{customerId}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Réactiver un client")
    public ResponseEntity<ApiResponse<Void>> activateCustomer(
            @PathVariable UUID customerId) {
        customerService.activateCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success("Client réactivé", null));
    }

    @PatchMapping("/{customerId}/loyalty/add/{points}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Ajouter des points de fidélité")
    public ResponseEntity<ApiResponse<CustomerResponse>> addLoyaltyPoints(
            @PathVariable UUID customerId,
            @PathVariable Integer points) {
        CustomerResponse response = customerService.addLoyaltyPoints(customerId, points);
        return ResponseEntity.ok(ApiResponse.success("Points de fidélité ajoutés", response));
    }

    @PatchMapping("/{customerId}/loyalty/remove/{points}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Retirer des points de fidélité")
    public ResponseEntity<ApiResponse<CustomerResponse>> removeLoyaltyPoints(
            @PathVariable UUID customerId,
            @PathVariable Integer points) {
        CustomerResponse response = customerService.removeLoyaltyPoints(customerId, points);
        return ResponseEntity.ok(ApiResponse.success("Points de fidélité retirés", response));
    }
}
