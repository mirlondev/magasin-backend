package org.odema.posnew.controller;

import com.itextpdf.text.DocumentException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.response.ApiResponse;
import org.odema.posnew.dto.response.InvoiceResponse;
import org.odema.posnew.service.InventoryService;
import org.odema.posnew.service.InvoiceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@RequiredArgsConstructor
@RestController
@RequestMapping("/invoices")
@Tag(name = "Invoices", description = "API de gestion des factures")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {


        private  final InvoiceService invoiceService;


    @PostMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Générer une facture pour une commande")
    public ResponseEntity<ApiResponse<InvoiceResponse>> generateInvoice(@PathVariable UUID orderId) {
        try {
            InvoiceResponse response = invoiceService.generateInvoice(orderId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Facture générée avec succès", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir une facture par son ID")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(@PathVariable UUID invoiceId) {
        InvoiceResponse response = invoiceService.getInvoiceById(invoiceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir la facture d'une commande")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByOrder(@PathVariable UUID orderId) {
        InvoiceResponse response = invoiceService.getInvoiceByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir une facture par son numéro")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByNumber(@PathVariable String invoiceNumber) {
        InvoiceResponse response = invoiceService.getInvoiceByNumber(invoiceNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les factures d'un client")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByCustomer(
            @PathVariable UUID customerId) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les factures d'un store")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByStore(
            @PathVariable UUID storeId) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les factures par statut")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByStatus(
            @PathVariable String status) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les factures par plage de dates")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<InvoiceResponse> responses = invoiceService.getInvoicesByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{invoiceId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Télécharger le PDF d'une facture")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable UUID invoiceId) {
        try {
            byte[] pdfBytes = invoiceService.generateInvoicePdf(invoiceId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "facture.pdf");
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/{invoiceId}/pdf-url")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Obtenir l'URL du PDF d'une facture")
    public ResponseEntity<ApiResponse<String>> getInvoicePdfUrl(@PathVariable UUID invoiceId) {
        try {
            String pdfUrl = invoiceService.getInvoicePdfUrl(invoiceId);
            return ResponseEntity.ok(ApiResponse.success(pdfUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{invoiceId}/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Mettre à jour le statut d'une facture")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoiceStatus(
            @PathVariable UUID invoiceId,
            @PathVariable String status) {
        InvoiceResponse response = invoiceService.updateInvoiceStatus(invoiceId, status);
        return ResponseEntity.ok(ApiResponse.success("Statut de la facture mis à jour", response));
    }

    @PatchMapping("/{invoiceId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Marquer une facture comme payée")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markInvoiceAsPaid(
            @PathVariable UUID invoiceId,
            @RequestParam String paymentMethod) {
        InvoiceResponse response = invoiceService.markInvoiceAsPaid(invoiceId, paymentMethod);
        return ResponseEntity.ok(ApiResponse.success("Facture marquée comme payée", response));
    }

    @PatchMapping("/{invoiceId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Annuler une facture")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancelInvoice(@PathVariable UUID invoiceId) {
        InvoiceResponse response = invoiceService.cancelInvoice(invoiceId);
        return ResponseEntity.ok(ApiResponse.success("Facture annulée", response));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir les factures en retard")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getOverdueInvoices() {
        List<InvoiceResponse> responses = invoiceService.getOverdueInvoices();
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/outstanding-amount")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Obtenir le montant total des factures impayées")
    public ResponseEntity<ApiResponse<Double>> getTotalOutstandingAmount() {
        Double amount = invoiceService.getTotalOutstandingAmount();
        return ResponseEntity.ok(ApiResponse.success(amount));
    }

    @PostMapping("/{invoiceId}/send-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHOP_MANAGER')")
    @Operation(summary = "Envoyer une facture par email")
    public ResponseEntity<ApiResponse<Void>> sendInvoiceByEmail(
            @PathVariable UUID invoiceId,
            @RequestParam String email) {
        try {
            invoiceService.sendInvoiceByEmail(invoiceId, email);
            return ResponseEntity.ok(ApiResponse.success("Facture envoyée par email", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de l'envoi de l'email: " + e.getMessage()));
        }
    }
}
