package org.odema.posnew.api.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.application.dto.response.ReceiptResponse;

import org.odema.posnew.domain.model.enums.ReceiptType;
import org.odema.posnew.domain.service.ReceiptService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipts", description = "Gestion des tickets de caisse")
public class ReceiptController {

    private final ReceiptService receiptService;



    @GetMapping("/{receiptId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Récupérer un ticket par ID")
    public ResponseEntity<ReceiptResponse> getReceiptById(
            @Parameter(description = "ID du ticket")
            @PathVariable UUID receiptId
    ) {
        log.debug("Récupération ticket ID: {}", receiptId);

        ReceiptResponse response = receiptService.getReceiptById(receiptId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{receiptNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Récupérer un ticket par numéro")
    public ResponseEntity<ReceiptResponse> getReceiptByNumber(
            @Parameter(description = "Numéro du ticket", example = "RCP-ST001-20260216-0001")
            @PathVariable String receiptNumber
    ) {
        log.debug("Récupération ticket numéro: {}", receiptNumber);

        ReceiptResponse response = receiptService.getReceiptByNumber(receiptNumber);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Récupérer le ticket d'une commande")
    public ResponseEntity<ReceiptResponse> getReceiptByOrder(
            @Parameter(description = "ID de la commande")
            @PathVariable UUID orderId
    ) {
        log.debug("Récupération ticket pour commande: {}", orderId);

        ReceiptResponse response = receiptService.getReceiptByOrder(orderId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{receiptId}/reprint")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(
            summary = "Réimprimer un ticket",
            description = "Réimprime un ticket existant. Incrémente le compteur d'impressions."
    )
    public ResponseEntity<ReceiptResponse> reprintReceipt(
            @Parameter(description = "ID du ticket")
            @PathVariable UUID receiptId
    ) throws IOException {
        log.info("Réimpression ticket ID: {}", receiptId);

        ReceiptResponse response = receiptService.reprintReceipt(receiptId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{receiptId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Télécharger le PDF du ticket")
    public ResponseEntity<byte[]> downloadReceiptPdf(
            @PathVariable UUID receiptId
    ) {
        log.debug("Téléchargement PDF ticket ID: {}", receiptId);

        try {
            byte[] pdfBytes = receiptService.generateReceiptPdf(receiptId);

            // Récupérer le numéro pour le nom du fichier
            ReceiptResponse receipt = receiptService.getReceiptById(receiptId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(
                    "attachment",
                    receipt.receiptNumber() + ".pdf"
            );
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Erreur téléchargement PDF ticket {}", receiptId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{receiptId}/thermal")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(
            summary = "Obtenir les données thermiques ESC/POS",
            description = "Retourne les données formatées pour imprimante thermique"
    )
    public ResponseEntity<String> getThermalData(
            @Parameter(description = "ID du ticket")
            @PathVariable UUID receiptId
    ) {
        log.debug("Récupération données thermiques ticket ID: {}", receiptId);

        String thermalData = receiptService.generateThermalData(receiptId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        return new ResponseEntity<>(thermalData, headers, HttpStatus.OK);
    }

    @PutMapping("/{receiptId}/void")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
            summary = "Annuler un ticket",
            description = "Annule un ticket. Action irréversible."
    )
    public ResponseEntity<ReceiptResponse> voidReceipt(
            @Parameter(description = "ID du ticket")
            @PathVariable UUID receiptId,

            @Parameter(description = "Raison de l'annulation")
            @RequestParam String reason
    ) {
        log.warn("Annulation ticket ID: {} - Raison: {}", receiptId, reason);

        ReceiptResponse response = receiptService.voidReceipt(receiptId, reason);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/shift/{shiftReportId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'MANAGER')")
    @Operation(summary = "Lister les tickets d'une session de caisse")
    public ResponseEntity<List<ReceiptResponse>> getReceiptsByShift(
            @Parameter(description = "ID de la session")
            @PathVariable UUID shiftReportId
    ) {
        log.debug("Récupération tickets session: {}", shiftReportId);

        List<ReceiptResponse> receipts = receiptService.getReceiptsByShift(shiftReportId);

        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lister les tickets d'un magasin par période")
    public ResponseEntity<List<ReceiptResponse>> getReceiptsByStore(
            @Parameter(description = "ID du magasin")
            @PathVariable UUID storeId,

            @Parameter(description = "Date de début (format: yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Date de fin (format: yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.debug("Récupération tickets magasin {} du {} au {}",
                storeId, startDate, endDate);

        List<ReceiptResponse> receipts = receiptService.getReceiptsByDateRange(
                storeId, startDate, endDate
        );

        return ResponseEntity.ok(receipts);
    }

    @GetMapping("/cashier/{cashierId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Lister les tickets d'un caissier par période")
    public ResponseEntity<List<ReceiptResponse>> getReceiptsByCashier(
            @Parameter(description = "ID du caissier")
            @PathVariable UUID cashierId,

            @Parameter(description = "Date de début")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "Date de fin")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.debug("Récupération tickets caissier {} du {} au {}",
                cashierId, startDate, endDate);

        List<ReceiptResponse> receipts = receiptService.getReceiptsByCashier(
                cashierId, startDate, endDate
        );

        return ResponseEntity.ok(receipts);
    }






    //new method

    @PostMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ReceiptResponse> generateReceipt(
            @PathVariable UUID orderId,
            @RequestParam(required = false, defaultValue = "SALE") ReceiptType type
    ) {  // ✅ plus de throws IOException
        log.info("Génération ticket pour commande {} - Type: {}", orderId, type);
        ReceiptResponse response = receiptService.generateReceipt(orderId, type);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ FIX 3 : ajouter les endpoints manquants pour les nouvelles méthodes du service
    @PostMapping("/order/{orderId}/payment-received")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(summary = "Générer un reçu de paiement reçu (crédit)")
    public ResponseEntity<ReceiptResponse> generatePaymentReceivedReceipt(
            @PathVariable UUID orderId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String notes
    ) {
        log.info("Génération reçu paiement reçu commande {} - Montant: {}", orderId, amount);
        ReceiptResponse response = receiptService.generatePaymentReceivedReceipt(
                orderId, amount, notes
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{receiptId}/void-receipt")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Générer un ticket d'annulation VOID")
    public ResponseEntity<ReceiptResponse> generateVoidReceipt(
            @PathVariable UUID receiptId,
            @RequestParam String reason
    ) {
        log.warn("Génération ticket VOID pour receipt {} - Raison: {}", receiptId, reason);
        ReceiptResponse response = receiptService.generateVoidReceipt(receiptId, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/order/{orderId}/delivery-note")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    @Operation(summary = "Générer un bon de livraison (ticket)")
    public ResponseEntity<ReceiptResponse> generateDeliveryNoteReceipt(
            @PathVariable UUID orderId
    ) {
        log.info("Génération bon de livraison (ticket) commande {}", orderId);
        ReceiptResponse response = receiptService.generateDeliveryNoteReceipt(orderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ FIX 4 : supprimer throws IOException sur les tickets shift
    @PostMapping("/shift/{shiftReportId}/opening")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ReceiptResponse> generateShiftOpeningReceipt(
            @PathVariable UUID shiftReportId
    ) {  // ✅ plus de throws IOException
        log.info("Génération ticket ouverture caisse session: {}", shiftReportId);
        ReceiptResponse response = receiptService.generateShiftOpeningReceipt(shiftReportId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/shift/{shiftReportId}/closing")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ReceiptResponse> generateShiftClosingReceipt(
            @PathVariable UUID shiftReportId
    ) {  // ✅ plus de throws IOException
        log.info("Génération ticket fermeture caisse session: {}", shiftReportId);
        ReceiptResponse response = receiptService.generateShiftClosingReceipt(shiftReportId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/shift/{shiftReportId}/cash-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ReceiptResponse> generateCashInReceipt(
            @PathVariable UUID shiftReportId,
            @RequestParam Double amount,
            @RequestParam String reason
    ) {  // ✅ plus de throws IOException
        ReceiptResponse response = receiptService.generateCashInReceipt(shiftReportId, amount, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/shift/{shiftReportId}/cash-out")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ReceiptResponse> generateCashOutReceipt(
            @PathVariable UUID shiftReportId,
            @RequestParam Double amount,
            @RequestParam String reason
    ) {  // ✅ plus de throws IOException
        ReceiptResponse response = receiptService.generateCashOutReceipt(shiftReportId, amount, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //new



}
