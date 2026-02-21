package org.odema.posnew.domain.service;

import org.odema.posnew.application.dto.response.ReceiptResponse;
import org.odema.posnew.domain.model.enums.ReceiptType;


import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReceiptService {

    /**
     * Génère un ticket pour une commande
     */
    ReceiptResponse generateReceipt(UUID orderId, ReceiptType type) ;

    /**
     * Récupère un ticket par son ID
     */
    ReceiptResponse getReceiptById(UUID receiptId);

    /**
     * Récupère un ticket par son numéro
     */
    ReceiptResponse getReceiptByNumber(String receiptNumber);

    /**
     * Récupère le ticket d'une commande
     */
    ReceiptResponse getReceiptByOrder(UUID orderId);

    /**
     * Réimprime un ticket (incrémente le compteur)
     */
    ReceiptResponse reprintReceipt(UUID receiptId) throws IOException;

    /**
     * Génère le PDF d'un ticket
     */
    byte[] generateReceiptPdf(UUID receiptId) throws IOException;

    /**
     * Génère les données thermiques ESC/POS
     */
    String generateThermalData(UUID receiptId);

    /**
     * Annule un ticket
     */
    ReceiptResponse voidReceipt(UUID receiptId, String reason);

    /**
     * Liste des tickets par shift
     */
    List<ReceiptResponse> getReceiptsByShift(UUID shiftReportId);

    /**
     * Liste des tickets par plage de dates
     */
    List<ReceiptResponse> getReceiptsByDateRange(UUID storeId,
                                                 LocalDate startDate,
                                                 LocalDate endDate);

    /**
     * Liste des tickets par caissier
     */
    List<ReceiptResponse> getReceiptsByCashier(UUID cashierId,
                                               LocalDate startDate,
                                               LocalDate endDate);

    /**
     * Génère un ticket d'ouverture de caisse
     */
    ReceiptResponse generateShiftOpeningReceipt(UUID shiftReportId);

    /**
     * Génère un ticket de fermeture de caisse
     */
    ReceiptResponse generateShiftClosingReceipt(UUID shiftReportId) ;

    /**
     * Génère un ticket d'entrée d'argent
     */
    ReceiptResponse generateCashInReceipt(UUID shiftReportId,
                                          Double amount,
                                          String reason);

    /**
     * Génère un ticket de sortie d'argent
     */
    ReceiptResponse generateCashOutReceipt(UUID shiftReportId,
                                           Double amount,
                                           String reason) ;

    // Cas manquants
    ReceiptResponse generatePaymentReceivedReceipt(UUID orderId, BigDecimal amount, String notes);
    ReceiptResponse generateVoidReceipt(UUID receiptId, String reason);
    ReceiptResponse generateDeliveryNoteReceipt(UUID orderId);
    }
