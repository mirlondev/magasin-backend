package org.odema.posnew.api.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.application.dto.request.PaymentRequest;
import org.odema.posnew.application.dto.response.ApiResponse;
import org.odema.posnew.application.dto.response.PaymentResponse;
import org.odema.posnew.api.exception.UnauthorizedException;
import org.odema.posnew.application.security.CustomUserDetails;
import org.odema.posnew.domain.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "API de gestion des paiements")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * ✅ Process payment for order (redirects to OrderService.addPaymentToOrder)
     * Note: This is now handled by OrderController at /orders/{orderId}/payments
     * Kept here for backward compatibility or direct payment processing
     */
    @PostMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Effectuer un paiement pour une commande")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws UnauthorizedException {

        PaymentResponse response = paymentService.processPayment(orderId, request, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Paiement enregistré avec succès", response));
    }

    @PostMapping("/orders/{orderId}/credit")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Créer un paiement à crédit (managers uniquement)")
    public ResponseEntity<ApiResponse<PaymentResponse>> createCreditPayment(
            @PathVariable UUID orderId,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) throws UnauthorizedException {

        PaymentResponse response = paymentService.createCreditPayment(orderId, request, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Crédit enregistré avec succès", response));
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir tous les paiements d'une commande")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getOrderPayments(@PathVariable UUID orderId) {
        List<PaymentResponse> responses = paymentService.getOrderPayments(orderId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @DeleteMapping("/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Annuler un paiement")
    public ResponseEntity<ApiResponse<Void>> cancelPayment(@PathVariable UUID paymentId) {
        paymentService.cancelPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Paiement annulé avec succès", null));
    }

    @GetMapping("/orders/{orderId}/total-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir le total payé pour une commande")
    public ResponseEntity<ApiResponse<BigDecimal>> getOrderTotalPaid(@PathVariable UUID orderId) {
        BigDecimal total = paymentService.getOrderTotalPaid(orderId);
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    @GetMapping("/orders/{orderId}/credit-amount")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir le montant en crédit pour une commande")
    public ResponseEntity<ApiResponse<BigDecimal>> getOrderCreditAmount(@PathVariable UUID orderId) {
        BigDecimal credit = paymentService.getOrderCreditAmount(orderId);
        return ResponseEntity.ok(ApiResponse.success(credit));
    }

    @GetMapping("/orders/{orderId}/remaining-amount")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir le montant restant à payer")
    public ResponseEntity<ApiResponse<BigDecimal>> getOrderRemainingAmount(@PathVariable UUID orderId) {
        BigDecimal remaining = paymentService.getOrderRemainingAmount(orderId);
        return ResponseEntity.ok(ApiResponse.success(remaining));
    }
}