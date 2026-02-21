
// controller/InvoiceController.java
package org.odema.posnew.api.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.application.dto.InvoiceResponse;
import org.odema.posnew.domain.service.InvoiceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Gestion des factures et proformas")
public class InvoiceController {

    private final InvoiceService invoiceService;



    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER', 'SALES')")
    @Operation(summary = "Récupérer une facture par ID")
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @Parameter(description = "ID de la facture")
            @PathVariable UUID invoiceId
    ) {
        log.debug("Récupération facture ID: {}", invoiceId);

        InvoiceResponse response = invoiceService.getInvoiceById(invoiceId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER', 'SALES')")
    @Operation(summary = "Récupérer une facture par numéro")
    public ResponseEntity<InvoiceResponse> getInvoiceByNumber(
            @Parameter(description = "Numéro de facture", example = "INV-202602-0001")
            @PathVariable String invoiceNumber
    ) {
        log.debug("Récupération facture numéro: {}", invoiceNumber);

        InvoiceResponse response = invoiceService.getInvoiceByNumber(invoiceNumber);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER', 'SALES')")
    @Operation(summary = "Récupérer la facture d'une commande")
    public ResponseEntity<InvoiceResponse> getInvoiceByOrder(
            @Parameter(description = "ID de la commande")
            @PathVariable UUID orderId
    ) {
        log.debug("Récupération facture pour commande: {}", orderId);

        InvoiceResponse response = invoiceService.getInvoiceByOrder(orderId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{invoiceId}/reprint")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'SALES')")
    @Operation(
            summary = "Réimprimer une facture",
            description = "Réimprime une facture existante. Incrémente le compteur."
    )
    public ResponseEntity<InvoiceResponse> reprintInvoice(
            @Parameter(description = "ID de la facture")
            @PathVariable UUID invoiceId
    ) {
        log.info("Réimpression facture ID: {}", invoiceId);

        InvoiceResponse response = invoiceService.reprintInvoice(invoiceId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{invoiceId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Mettre à jour le statut d'une facture")
    public ResponseEntity<InvoiceResponse> updateInvoiceStatus(
            @Parameter(description = "ID de la facture")
            @PathVariable UUID invoiceId,

            @Parameter(description = "Nouveau statut", example = "PAID")
            @RequestParam String status
    ) {
        log.info("Mise à jour statut facture {} vers {}", invoiceId, status);

        InvoiceResponse response = invoiceService.updateInvoiceStatus(invoiceId, status);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{invoiceId}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(summary = "Marquer une facture comme payée")
    public ResponseEntity<InvoiceResponse> markInvoiceAsPaid(
            @Parameter(description = "ID de la facture")
            @PathVariable UUID invoiceId,

            @Parameter(description = "Méthode de paiement")
            @RequestParam String paymentMethod
    ) {
        log.info("Marquage facture {} comme payée - Méthode: {}",
                invoiceId, paymentMethod);

        InvoiceResponse response = invoiceService.markInvoiceAsPaid(
                invoiceId, paymentMethod
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{invoiceId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Annuler une facture")
    public ResponseEntity<InvoiceResponse> cancelInvoice(
            @Parameter(description = "ID de la facture")
            @PathVariable UUID invoiceId
    ) {
        log.warn("Annulation facture ID: {}", invoiceId);

        InvoiceResponse response = invoiceService.cancelInvoice(invoiceId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{proformaId}/convert-to-sale")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    @Operation(
            summary = "Convertir un proforma en vente",
            description = "Convertit un proforma en commande réelle avec facture"
    )
    public ResponseEntity<InvoiceResponse> convertProformaToSale(
            @Parameter(description = "ID du proforma")
            @PathVariable UUID proformaId
    ) {
        log.info("Conversion proforma {} en vente", proformaId);

        InvoiceResponse response = invoiceService.convertProformaToSale(proformaId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'SALES')")
    @Operation(summary = "Lister les factures d'un client")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByCustomer(
            @Parameter(description = "ID du client")
            @PathVariable UUID customerId
    ) {
        log.debug("Récupération factures client: {}", customerId);

        List<InvoiceResponse> invoices = invoiceService.getInvoicesByCustomer(customerId);

        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lister les factures d'un magasin")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByStore(
            @Parameter(description = "ID du magasin")
            @PathVariable UUID storeId
    ) {
        log.debug("Récupération factures magasin: {}", storeId);

        List<InvoiceResponse> invoices = invoiceService.getInvoicesByStore(storeId);

        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lister les factures par statut")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByStatus(
            @Parameter(description = "Statut", example = "ISSUED")
            @PathVariable String status
    ) {
        log.debug("Récupération factures statut: {}", status);

        List<InvoiceResponse> invoices = invoiceService.getInvoicesByStatus(status);

        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lister les factures par période")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByDateRange(
            @Parameter(description = "Date de début")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Date de fin")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.debug("Récupération factures du {} au {}", startDate, endDate);

        List<InvoiceResponse> invoices = invoiceService.getInvoicesByDateRange(
                startDate, endDate
        );

        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lister les factures en retard")
    public ResponseEntity<List<InvoiceResponse>> getOverdueInvoices() {
        log.debug("Récupération factures en retard");

        List<InvoiceResponse> invoices = invoiceService.getOverdueInvoices();

        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/outstanding-amount")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Obtenir le montant total des créances")
    public ResponseEntity<Double> getTotalOutstandingAmount() {
        log.debug("Récupération montant total créances");

        Double amount = invoiceService.getTotalOutstandingAmount();

        return ResponseEntity.ok(amount);
    }

    @PostMapping("/{invoiceId}/send-email")
    @PreAuthorize("hasAnyRole('ADMIN', 'SALES')")
    @Operation(summary = "Envoyer une facture par email")
    public ResponseEntity<Void> sendInvoiceByEmail(
            @Parameter(description = "ID de la facture")
            @PathVariable UUID invoiceId,

            @Parameter(description = "Email destinataire")
            @RequestParam String email
    ) {
        log.info("Envoi facture {} à {}", invoiceId, email);

        try {
            invoiceService.sendInvoiceByEmail(invoiceId, email);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Erreur envoi email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/order/{orderId}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdfOfOrder(@PathVariable UUID orderId) {
        try {
            // This will check disk first, generate only if needed
            byte[] pdfBytes = invoiceService.getOrGenerateInvoicePdf(orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("inline")
                    .filename("facture.pdf")
                    .build());

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur téléchargement PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    //thew

    @PostMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'SALES')")
    public ResponseEntity<InvoiceResponse> generateInvoice(
            @PathVariable UUID orderId
    ) {  // ✅ plus de throws
        log.info("Génération facture pour commande: {}", orderId);
        InvoiceResponse response = invoiceService.generateInvoice(orderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ FIX 2 : regenerateInvoicePdf() — ne pas retourner null
    @PostMapping("/order/{orderId}/pdf/regenerate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Forcer la régénération du PDF d'une facture")
    public ResponseEntity<byte[]> regenerateInvoicePdf(@PathVariable UUID orderId) {
        try {
            byte[] pdfBytes = invoiceService.regenerateInvoicePdf(orderId);

            InvoiceResponse invoice = invoiceService.getInvoiceByOrder(orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename(invoice.invoiceNumber() + ".pdf")
                    .build());
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur régénération PDF facture commande {}", orderId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ FIX 3 : ajouter endpoints manquants pour les types de factures spéciaux
    @GetMapping("/order/{orderId}/proforma/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'SALES')")
    @Operation(summary = "Télécharger le PDF proforma d'une commande")
    public ResponseEntity<byte[]> downloadProformaPdf(@PathVariable UUID orderId) {
        try {
            byte[] pdfBytes = invoiceService.getOrGenerateProformaPdf(orderId);
            return buildPdfResponse(pdfBytes, "proforma_" + orderId + ".pdf");
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur téléchargement PDF proforma", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/order/{orderId}/credit-note/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Télécharger le PDF avoir d'une commande")
    public ResponseEntity<byte[]> downloadCreditNotePdf(@PathVariable UUID orderId) {
        try {
            byte[] pdfBytes = invoiceService.getOrGenerateCreditNotePdf(orderId);
            return buildPdfResponse(pdfBytes, "avoir_" + orderId + ".pdf");
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur téléchargement PDF avoir", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/order/{orderId}/delivery-note/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(summary = "Télécharger le PDF bon de livraison d'une commande")
    public ResponseEntity<byte[]> downloadDeliveryNotePdf(@PathVariable UUID orderId) {
        try {
            byte[] pdfBytes = invoiceService.getOrGenerateDeliveryNotePdf(orderId);
            return buildPdfResponse(pdfBytes, "bon_livraison_" + orderId + ".pdf");
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur téléchargement PDF bon de livraison", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ✅ Méthode utilitaire privée — évite la duplication des headers PDF
    private ResponseEntity<byte[]> buildPdfResponse(byte[] pdfBytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("inline")
                .filename(filename)
                .build());
        headers.setContentLength(pdfBytes.length);
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}