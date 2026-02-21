package org.odema.posnew.api.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.application.dto.request.CreateOrderWithPaymentRequest;
import org.odema.posnew.application.dto.request.OrderRequest;
import org.odema.posnew.application.dto.request.PaymentRequest;
import org.odema.posnew.application.dto.response.ApiResponse;
import org.odema.posnew.application.dto.response.OrderResponse;
import org.odema.posnew.application.dto.response.PaginatedResponse;
import org.odema.posnew.api.rest.exception.UnauthorizedException;
import org.odema.posnew.application.security.CustomUserDetails;
import org.odema.posnew.application.service.OrderService;
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
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;

    /**
     * ✅ Create order WITHOUT payment
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Créer une nouvelle commande (sans paiement)")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws UnauthorizedException {

        UUID cashierId = userDetails.getUserId();
        OrderResponse response = orderService.createOrder(request, cashierId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Commande créée avec succès", response));
    }

    /**
     * ✅ Create order WITH initial payment (convenience endpoint)
     */
    @PostMapping("/with-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Créer une commande avec paiement initial")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrderWithPayment(
            @Valid @RequestBody CreateOrderWithPaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws UnauthorizedException {

        UUID cashierId = userDetails.getUserId();
        OrderResponse response = orderService.createOrderWithPayment(
                request.orderRequest(),
                request.paymentRequest(),
                cashierId
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Commande et paiement créés avec succès", response));
    }

    /**
     * ✅ Add payment to existing order
     */
    @PostMapping("/{orderId}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Ajouter un paiement à une commande existante")
    public ResponseEntity<ApiResponse<OrderResponse>> addPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentRequest paymentRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws UnauthorizedException {

        // DEBUG LOGGING
        log.info("Received payment request - Order: {}, Method: {}, Amount: {}",
                orderId, paymentRequest.method(), paymentRequest.amount());

        UUID cashierId = userDetails.getUserId();
        OrderResponse response = orderService.addPaymentToOrder(orderId, paymentRequest, cashierId);

        log.info("Payment processed - Order status: {}, Payment status: {}",
                response.status(), response.paymentStatus());

        return ResponseEntity.ok(ApiResponse.success("Paiement ajouté avec succès", response));
    }
    /*public ResponseEntity<ApiResponse<OrderResponse>> addPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentRequest paymentRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws UnauthorizedException {

        UUID cashierId = userDetails.getUserId();
        OrderResponse response = orderService.addPaymentToOrder(orderId, paymentRequest, cashierId);
        return ResponseEntity.ok(ApiResponse.success("Paiement ajouté avec succès", response));
    }*/

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir une commande par son ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID orderId) {
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir toutes les commandes (paginé)")
    public ResponseEntity<ApiResponse<PaginatedResponse<OrderResponse>>> getAllOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {

        UUID userId = userDetails.getUserId();
        Page<OrderResponse> responses = orderService.getOrders(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(responses)));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les commandes d'un store")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByStore(
            @PathVariable UUID storeId) {
        List<OrderResponse> responses = orderService.getOrdersByStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les commandes d'un client")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByCustomer(
            @PathVariable UUID customerId) {
        List<OrderResponse> responses = orderService.getOrdersByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les commandes par statut")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByStatus(
            @PathVariable String status) {
        List<OrderResponse> responses = orderService.getOrdersByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les commandes par plage de dates")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<OrderResponse> responses = orderService.getOrdersByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PatchMapping("/{orderId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Marquer une commande comme terminée")
    public ResponseEntity<ApiResponse<OrderResponse>> markAsCompleted(@PathVariable UUID orderId) {
        OrderResponse response = orderService.markAsCompleted(orderId);
        return ResponseEntity.ok(ApiResponse.success("Commande terminée", response));
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Annuler une commande")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable UUID orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("Commande annulée", null));
    }

    @PutMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Mettre à jour une commande")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.updateOrder(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Commande mise à jour", response));
    }

    @GetMapping("/recent/{limit}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les commandes récentes")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getRecentOrders(@PathVariable int limit) {
        List<OrderResponse> responses = orderService.getRecentOrders(limit);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/cashier/{cashierId}/shift/{shiftId}")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_ADMIN','SHOP_MANAGER') or #cashierId == authentication.principal.userId")
    @Operation(summary = "Obtenir les commandes d'un caissier durant un shift")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getCashierOrdersByShift(
            @PathVariable UUID cashierId,
            @PathVariable UUID shiftId) {
        List<OrderResponse> responses = orderService.findCashierOrdersByShift(cashierId, shiftId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/store/{storeId}/sales-total")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir le total des ventes d'un store")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalSalesByStore(
            @PathVariable UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        BigDecimal total = orderService.getTotalSalesByStore(storeId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(total));
    }
}