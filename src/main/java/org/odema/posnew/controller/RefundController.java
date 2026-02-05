package org.odema.posnew.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.RefundRequest;
import org.odema.posnew.dto.response.ApiResponse;
import org.odema.posnew.dto.response.RefundResponse;
import org.odema.posnew.service.RefundService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "API de gestion des remboursements")
@SecurityRequirement(name = "bearerAuth")
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Créer un nouveau remboursement")
    public ResponseEntity<ApiResponse<RefundResponse>> createRefund(
            @Valid @RequestBody RefundRequest request,
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        UUID cashierId = UUID.fromString(userDetails.getUsername()); // À adapter
        RefundResponse response = refundService.createRefund(request, cashierId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Demande de remboursement créée", response));
    }

    @GetMapping("/{refundId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir un remboursement par son ID")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefund(
            @PathVariable UUID refundId) {
        RefundResponse response = refundService.getRefundById(refundId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir tous les remboursements")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getAllRefunds() {
        List<RefundResponse> responses = refundService.getAllRefunds();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les remboursements d'une commande")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getRefundsByOrder(
            @PathVariable UUID orderId) {
        List<RefundResponse> responses = refundService.getRefundsByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les remboursements d'un store")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getRefundsByStore(
            @PathVariable UUID storeId) {
        List<RefundResponse> responses = refundService.getRefundsByStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir les remboursements par statut")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getRefundsByStatus(
            @PathVariable String status) {
        List<RefundResponse> responses = refundService.getRefundsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PatchMapping("/{refundId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Approuver un remboursement")
    public ResponseEntity<ApiResponse<RefundResponse>> approveRefund(
            @PathVariable UUID refundId) {
        RefundResponse response = refundService.approveRefund(refundId);
        return ResponseEntity.ok(ApiResponse.success("Remboursement approuvé", response));
    }

    @PatchMapping("/{refundId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Rejeter un remboursement")
    public ResponseEntity<ApiResponse<RefundResponse>> rejectRefund(
            @PathVariable UUID refundId,
            @RequestParam String reason) {
        RefundResponse response = refundService.rejectRefund(refundId, reason);
        return ResponseEntity.ok(ApiResponse.success("Remboursement rejeté", response));
    }

    @PatchMapping("/{refundId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Compléter un remboursement")
    public ResponseEntity<ApiResponse<RefundResponse>> completeRefund(
            @PathVariable UUID refundId) {
        RefundResponse response = refundService.completeRefund(refundId);
        return ResponseEntity.ok(ApiResponse.success("Remboursement complété", response));
    }

    @GetMapping("/order/{orderId}/refundable")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Vérifier si une commande peut être remboursée")
    public ResponseEntity<ApiResponse<Boolean>> canOrderBeRefunded(
            @PathVariable UUID orderId) {
        boolean canBeRefunded = refundService.canOrderBeRefunded(orderId);
        return ResponseEntity.ok(ApiResponse.success(canBeRefunded));
    }

    @GetMapping("/order/{orderId}/refundable-amount")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir le montant remboursable d'une commande")
    public ResponseEntity<ApiResponse<BigDecimal>> getRefundableAmount(
            @PathVariable UUID orderId) {
        BigDecimal amount = refundService.getRefundableAmount(orderId);
        return ResponseEntity.ok(ApiResponse.success(amount));
    }

    @DeleteMapping("/{refundId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Annuler un remboursement")
    public ResponseEntity<ApiResponse<Void>> cancelRefund(@PathVariable UUID refundId) {
        refundService.cancelRefund(refundId);
        return ResponseEntity.ok(ApiResponse.success("Remboursement annulé", null));
    }
}
