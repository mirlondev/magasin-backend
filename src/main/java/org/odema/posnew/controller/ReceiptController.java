package org.odema.posnew.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.response.ApiResponse;
import org.odema.posnew.dto.response.ReceiptResponse;
import org.odema.posnew.service.ReceiptService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipts", description = "API de gestion des tickets de caisse")
@SecurityRequirement(name = "bearerAuth")
public class ReceiptController {

    private final ReceiptService receiptService;

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Obtenir les données de ticket pour une commande")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceipt(@PathVariable UUID orderId) {
        ReceiptResponse response = receiptService.generateReceipt(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/order/{orderId}/text")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Obtenir le ticket en format texte pour imprimante thermique")
    public ResponseEntity<String> getReceiptText(@PathVariable UUID orderId) {
        String receiptText = receiptService.getReceiptText(orderId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        return new ResponseEntity<>(receiptText, headers, HttpStatus.OK);
    }

    @GetMapping("/order/{orderId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Télécharger le ticket en PDF")
    public ResponseEntity<byte[]> downloadReceiptPdf(@PathVariable UUID orderId) {
        byte[] pdfBytes = receiptService.generateReceiptPdf(orderId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "ticket-" + orderId + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/order/{orderId}/thermal")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_ADMIN', 'CASHIER')")
    @Operation(summary = "Obtenir les données formatées pour imprimante thermique ESC/POS")
    public ResponseEntity<byte[]> getThermalPrinterData(@PathVariable UUID orderId) {
        // Format ESC/POS pour imprimantes thermiques
        byte[] thermalData = receiptService.generateThermalPrinterData(orderId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "ticket-thermal-" + orderId + ".bin");

        return new ResponseEntity<>(thermalData, headers, HttpStatus.OK);
    }
}