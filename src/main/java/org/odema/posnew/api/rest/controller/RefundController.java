package org.odema.posnew.api.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.application.dto.request.RefundRequest;
import org.odema.posnew.application.dto.response.RefundResponse;
import org.odema.posnew.domain.enums_old.RefundStatus;
import org.odema.posnew.application.service.RefundService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "Gestion des remboursements")
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Créer un remboursement")
    public ResponseEntity<RefundResponse> createRefund(
            @RequestBody RefundRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID cashierId = extractUserId(userDetails);
        log.info("Création remboursement par {}", cashierId);
        RefundResponse response = refundService.createRefund(request, cashierId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{refundId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Récupérer un remboursement par ID")
    public ResponseEntity<RefundResponse> getRefundById(
            @Parameter(description = "ID du remboursement")
            @PathVariable UUID refundId) {
        return ResponseEntity.ok(refundService.getRefundById(refundId));
    }

    @GetMapping("/number/{refundNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Récupérer un remboursement par numéro")
    public ResponseEntity<RefundResponse> getRefundByNumber(
            @Parameter(description = "Numéro du remboursement")
            @PathVariable String refundNumber) {
        return ResponseEntity.ok(refundService.getRefundByNumber(refundNumber));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Lister les remboursements d'une commande")
    public ResponseEntity<List<RefundResponse>> getRefundsByOrder(
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(refundService.getRefundsByOrder(orderId));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lister les remboursements d'un magasin")
    public ResponseEntity<List<RefundResponse>> getRefundsByStore(
            @PathVariable UUID storeId) {
        return ResponseEntity.ok(refundService.getRefundsByStore(storeId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lister les remboursements par statut")
    public ResponseEntity<List<RefundResponse>> getRefundsByStatus(
            @Parameter(description = "Statut du remboursement")
            @PathVariable RefundStatus status) {
        return ResponseEntity.ok(refundService.getRefundsByStatus(status));
    }

    @GetMapping("/store/{storeId}/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lister les remboursements par période")
    public ResponseEntity<List<RefundResponse>> getRefundsByDateRange(
            @PathVariable UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(refundService.getRefundsByDateRange(storeId, startDate, endDate));
    }

    @PostMapping("/{refundId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Approuver un remboursement")
    public ResponseEntity<RefundResponse> approveRefund(
            @PathVariable UUID refundId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID approverId = extractUserId(userDetails);
        log.info("Approbation remboursement {} par {}", refundId, approverId);
        return ResponseEntity.ok(refundService.approveRefund(refundId, approverId));
    }

    @PostMapping("/{refundId}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Traiter un remboursement approuvé")
    public ResponseEntity<RefundResponse> processRefund(
            @PathVariable UUID refundId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID processorId = extractUserId(userDetails);
        log.info("Traitement remboursement {} par {}", refundId, processorId);
        return ResponseEntity.ok(refundService.processRefund(refundId, processorId));
    }

    @PostMapping("/{refundId}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Compléter un remboursement")
    public ResponseEntity<RefundResponse> completeRefund(
            @PathVariable UUID refundId) {
        log.info("Complétion remboursement {}", refundId);
        return ResponseEntity.ok(refundService.completeRefund(refundId));
    }

    @PostMapping("/{refundId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Rejeter un remboursement")
    public ResponseEntity<RefundResponse> rejectRefund(
            @PathVariable UUID refundId,
            @RequestParam String reason) {
        log.warn("Rejet remboursement {} - Raison: {}", refundId, reason);
        return ResponseEntity.ok(refundService.rejectRefund(refundId, reason));
    }

    @PostMapping("/{refundId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Annuler un remboursement")
    public ResponseEntity<RefundResponse> cancelRefund(
            @PathVariable UUID refundId,
            @RequestParam String reason) {
        log.warn("Annulation remboursement {} - Raison: {}", refundId, reason);
        return ResponseEntity.ok(refundService.cancelRefund(refundId, reason));
    }

    @GetMapping("/{refundId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Télécharger le PDF du remboursement")
    public ResponseEntity<byte[]> downloadRefundPdf(
            @PathVariable UUID refundId) {
        byte[] pdf = refundService.generateRefundPdf(refundId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "remboursement_" + refundId + ".pdf");

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @GetMapping("/store/{storeId}/total")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Obtenir le total des remboursements sur une période")
    public ResponseEntity<BigDecimal> getTotalRefunds(
            @PathVariable UUID storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(refundService.getTotalRefundsByPeriod(storeId, startDate, endDate));
    }

    @GetMapping("/pending/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Compter les remboursements en attente")
    public ResponseEntity<Long> countPendingRefunds() {
        return ResponseEntity.ok(refundService.countPendingRefunds());
    }

    @GetMapping("/shift/{shiftReportId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Lister les remboursements d'une session de caisse")
    public ResponseEntity<List<RefundResponse>> getRefundsByShift(
            @PathVariable UUID shiftReportId) {
        return ResponseEntity.ok(refundService.getRefundsByShift(shiftReportId));
    }

    private UUID extractUserId(UserDetails userDetails) {
        // Adapter selon votre implémentation d'authentification
        return UUID.fromString(userDetails.getUsername());
    }
}
