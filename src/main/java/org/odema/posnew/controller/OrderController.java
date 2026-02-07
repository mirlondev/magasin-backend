package org.odema.posnew.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.OrderRequest;
import org.odema.posnew.dto.response.ApiResponse;
import org.odema.posnew.dto.response.OrderResponse;
import org.odema.posnew.dto.response.PaginatedResponse;
import org.odema.posnew.security.CustomUserDetails;
import org.odema.posnew.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "API de gestion des commandes")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Créer une nouvelle commande")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // Récupérer l'ID du caissier depuis les détails de l'utilisateur
        UUID cashierId = userDetails.getUserId(); // À adapter selon votre implémentation
        OrderResponse response = orderService.createOrder(request, cashierId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Commande créée avec succès", response));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir une commande par son ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable UUID orderId) {
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir toutes les commandes")
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderResponse>>> getAllOrders(@AuthenticationPrincipal CustomUserDetails userDetails, Pageable pageable) {
        UUID userId = userDetails.getUserId();
        Page<OrderResponse> responses = orderService.getOrders(userId,pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(responses)));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les commandes d'un store")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByStore(
            @PathVariable UUID storeId) {
        List<OrderResponse> responses = orderService.getOrdersByStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les commandes d'un client")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByCustomer(
            @PathVariable UUID customerId) {
        List<OrderResponse> responses = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les commandes par statut")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByStatus(
            @PathVariable String status) {
        List<OrderResponse> responses = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les commandes par plage de dates")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<OrderResponse> responses = orderService.getOrdersByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PatchMapping("/{orderId}/payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Traiter le paiement d'une commande")
    public ResponseEntity<ApiResponse<OrderResponse>> processPayment(
            @PathVariable UUID orderId,
            @RequestParam BigDecimal amountPaid) {
        OrderResponse response = orderService.processPayment(orderId, amountPaid);
        return ResponseEntity.ok(ApiResponse.success("Paiement traité", response));
    }

    @PatchMapping("/{orderId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Marquer une commande comme terminée")
    public ResponseEntity<ApiResponse<OrderResponse>> markAsCompleted(
            @PathVariable UUID orderId) {
        OrderResponse response = orderService.markAsCompleted(orderId);
        return ResponseEntity.ok(ApiResponse.success("Commande terminée", response));
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Annuler une commande")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable UUID orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("Commande annulée", null));
    }

    @GetMapping("/recent/{limit}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les commandes récentes")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getRecentOrders(
            @PathVariable int limit) {
        List<OrderResponse> responses = orderService.getRecentOrders(limit);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/store/{storeId}/sales-total")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir le total des ventes d'un store")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalSalesByStore(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        BigDecimal total = orderService.getTotalSalesByStore(storeId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(total));
    }
}
