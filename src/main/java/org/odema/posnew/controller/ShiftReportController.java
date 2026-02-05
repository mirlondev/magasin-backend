package org.odema.posnew.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.ShiftReportRequest;
import org.odema.posnew.dto.response.ApiResponse;
import org.odema.posnew.dto.response.ShiftReportResponse;
import org.odema.posnew.security.CustomUserDetails;
import org.odema.posnew.service.ShiftReportService;
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
@RequestMapping("/shift-reports")
@RequiredArgsConstructor
@Tag(name = "Shift Reports", description = "API de gestion des rapports de shift")
@SecurityRequirement(name = "bearerAuth")
public class ShiftReportController {

    private final ShiftReportService shiftReportService;

    @PostMapping("/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Ouvrir un nouveau shift")
    public ResponseEntity<ApiResponse<ShiftReportResponse>> openShift(
            @Valid @RequestBody ShiftReportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // Now we can directly get the UUID!
        UUID cashierId = userDetails.getUserId();
        ShiftReportResponse response = shiftReportService.openShift(request, cashierId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shift ouvert avec succès", response));
    }

    @PatchMapping("/{shiftReportId}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Fermer un shift")
    public ResponseEntity<ApiResponse<ShiftReportResponse>> closeShift(
            @PathVariable UUID shiftReportId,
            @RequestParam BigDecimal closingBalance,
            @RequestParam BigDecimal actualBalance) {
        ShiftReportResponse response = shiftReportService.closeShift(shiftReportId, closingBalance, actualBalance);
        return ResponseEntity.ok(ApiResponse.success("Shift fermé avec succès", response));
    }

    @GetMapping("/{shiftReportId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Obtenir un shift par son ID")
    public ResponseEntity<ApiResponse<ShiftReportResponse>> getShiftReport(
            @PathVariable UUID shiftReportId) {
        ShiftReportResponse response = shiftReportService.getShiftReportById(shiftReportId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Obtenir tous les shifts")
    public ResponseEntity<ApiResponse<List<ShiftReportResponse>>> getAllShiftReports() {
        List<ShiftReportResponse> responses = shiftReportService.getShiftsByCashier(null);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/cashier/{cashierId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Obtenir les shifts d'un caissier")
    public ResponseEntity<ApiResponse<List<ShiftReportResponse>>> getShiftsByCashier(
            @PathVariable UUID cashierId) {
        List<ShiftReportResponse> responses = shiftReportService.getShiftsByCashier(cashierId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Obtenir les shifts d'un store")
    public ResponseEntity<ApiResponse<List<ShiftReportResponse>>> getShiftsByStore(
            @PathVariable UUID storeId) {
        List<ShiftReportResponse> responses = shiftReportService.getShiftsByStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Obtenir les shifts par statut")
    public ResponseEntity<ApiResponse<List<ShiftReportResponse>>> getShiftsByStatus(
            @PathVariable String status) {
        List<ShiftReportResponse> responses = shiftReportService.getShiftsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/cashier/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Obtenir le shift ouvert du caissier courant")
    public ResponseEntity<ApiResponse<ShiftReportResponse>> getOpenShift(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // Direct access to user UUID - much cleaner!
        UUID cashierId = userDetails.getUserId();
        ShiftReportResponse response = shiftReportService.getOpenShiftByCashier(cashierId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/store/{storeId}/open")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Obtenir les shifts ouverts d'un store")
    public ResponseEntity<ApiResponse<List<ShiftReportResponse>>> getOpenShiftsByStore(
            @PathVariable UUID storeId) {
        List<ShiftReportResponse> responses = shiftReportService.getOpenShiftsByStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Obtenir les shifts par plage de dates")
    public ResponseEntity<ApiResponse<List<ShiftReportResponse>>> getShiftsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<ShiftReportResponse> responses = shiftReportService.getShiftsByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{shiftReportId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Mettre à jour un shift")
    public ResponseEntity<ApiResponse<ShiftReportResponse>> updateShiftReport(
            @PathVariable UUID shiftReportId,
            @Valid @RequestBody ShiftReportRequest request) {
        ShiftReportResponse response = shiftReportService.updateShiftReport(shiftReportId, request);
        return ResponseEntity.ok(ApiResponse.success("Shift mis à jour", response));
    }

    @PatchMapping("/{shiftReportId}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Suspendre un shift")
    public ResponseEntity<ApiResponse<Void>> suspendShift(
            @PathVariable UUID shiftReportId,
            @RequestParam(required = false) String reason) {
        shiftReportService.suspendShift(shiftReportId, reason);
        return ResponseEntity.ok(ApiResponse.success("Shift suspendu", null));
    }

    @PatchMapping("/{shiftReportId}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Reprendre un shift")
    public ResponseEntity<ApiResponse<ShiftReportResponse>> resumeShift(
            @PathVariable UUID shiftReportId) {
        ShiftReportResponse response = shiftReportService.resumeShift(shiftReportId);
        return ResponseEntity.ok(ApiResponse.success("Shift repris", response));
    }

    @GetMapping("/store/{storeId}/sales-total")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Obtenir le total des ventes d'un store (tous shifts)")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalSalesByStore(
            @PathVariable UUID storeId) {
        BigDecimal total = shiftReportService.getTotalSalesByStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(total));
    }

    @GetMapping("/store/{storeId}/refunds-total")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN')")
    @Operation(summary = "Obtenir le total des remboursements d'un store (tous shifts)")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalRefundsByStore(
            @PathVariable UUID storeId) {
        BigDecimal total = shiftReportService.getTotalRefundsByStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(total));
    }
}