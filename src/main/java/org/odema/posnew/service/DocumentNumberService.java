package org.odema.posnew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.entity.enums.DocumentType;
import org.odema.posnew.entity.enums.InvoiceType;
import org.odema.posnew.entity.enums.ReceiptType;
import org.odema.posnew.repository.InvoiceRepository;
import org.odema.posnew.repository.OrderRepository;
import org.odema.posnew.repository.ReceiptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;
import java.util.function.LongFunction;
import java.util.function.Predicate;

/**
 * Service centralisé pour générer des numéros de documents uniques.
 * Tous les documents (tickets, factures, proformas, bons de livraison)
 * reçoivent un numéro généré UNE SEULE FOIS, réimprimable à l'infini.
 *
 * Formats:
 *  - Ticket vente       → RCP-ST001-20260217-0001
 *  - Ticket fermeture   → CLS-ST001-20260217-0001
 *  - Entrée caisse      → CIN-ST001-20260217-0001
 *  - Sortie caisse      → COT-ST001-20260217-0001
 *  - Facture crédit     → INV-202602-0001
 *  - Proforma / Devis   → PRO-202602-0001
 *  - Bon de livraison   → BL-202602-0001
 *  - Avoir              → AV-202602-0001
 *  - Commande           → ORD-20260217-0001
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentNumberService {

    private final ReceiptRepository receiptRepository;
    private final InvoiceRepository invoiceRepository;
    private final OrderRepository   orderRepository;

    // =========================================================================
    // TICKETS (Receipts)
    // =========================================================================

    /**
     * Génère un numéro de ticket UNIQUE.
     * Format: {PREFIX}-{STORE_CODE}-{YYYYMMDD}-{SEQ:0000}
     * Exemple: RCP-ST0F3A-20260217-0001
     **/
    @Transactional
    public synchronized String generateReceiptNumber(UUID storeId, ReceiptType type) {
        return getReceiptPrefix(storeId, type);
    }

    /**
     * Convertit un UUID de magasin en code court de 6 caractères (Base64 URL-safe).
     * Exemple: "550e8400-e29b-41d4-a716-446655440000" → "VgQ-gq5B"
     */
    private String buildStoreCode(UUID storeId) {
        if (storeId == null) {
            return "UNKNWN"; // Fallback pour les cas edge case
        }

        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(storeId.getMostSignificantBits());
        buffer.putLong(storeId.getLeastSignificantBits());

        // Encodage Base64 URL-safe sans padding (22 chars → on prend les 6 premiers)
        String base64 = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(buffer.array());

        // On prend les 6 premiers caractères pour un code court et lisible
        // Avec 6 caractères Base64 = 36 bits d'entropie, suffisant pour éviter les collisions
        // parmi un nombre raisonnable de magasins (milliers)
        String shortCode = base64.substring(0, 6).toUpperCase();

        // Remplacer les caractères ambigus pour une meilleure lisibilité humaine
        return shortCode.replace('0', 'Z')
                .replace('O', 'X')
                .replace('I', 'Y')
                .replace('l', 'K');
    }



    // =========================================================================
    // FACTURES (Invoices)
    // =========================================================================

    /**
     * Génère un numéro de facture/proforma/BL UNIQUE.
     * Format: {PREFIX}-{YYYYMM}-{SEQ:0000}
     * Exemple: INV-202602-0001 / PRO-202602-0001 / BL-202602-0001
     */
    @Transactional
    public synchronized String generateInvoiceNumber(InvoiceType type) {
        return getPrefix(type);
    }

    /**
     * Retourne le préfixe correspondant au type de facture.
     */
    private String getInvoicePrefix( InvoiceType type) {
        return switch (type) {
            case INVOICE           -> "INV";  // Facture standard
            case CREDIT_SALE        -> "CS"; //vente a credit
            case PROFORMA          -> "PRO";  // Proforma / Devis
            case CREDIT_NOTE       -> "AV";   // Avoir (Note de crédit)
            case DELIVERY_NOTE     -> "BL";   // Bon de livraison
            case PURCHASE_ORDER    -> "PO";   // Bon de commande fournisseur
            case QUOTE             -> "QUO";  // Devis officiel
            case INVOICE_CORRECTED -> "CRR";  // Facture rectificative
        };
    }

    // =========================================================================
    // COMMANDES (Orders)
    // =========================================================================

    /**
     * Génère un numéro de commande UNIQUE.
     * Format: ORD-{YYYYMMDD}-{SEQ:0000}
     * Exemple: ORD-20260217-0001
     */
    @Transactional
    public synchronized String generateOrderNumber() {
        LocalDate today   = LocalDate.now();
        String    dateStr = String.format("%04d%02d%02d",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth());

        long count = orderRepository.countByCreatedAtDate(today);

        return resolveUniqueNumber(
                (seq) -> String.format("ORD-%s-%04d", dateStr, seq),
                orderRepository::existsByOrderNumber,
                count
        );
    }

    // =========================================================================
    // RÉSOLUTION D'UNICITÉ (méthode générique)
    // =========================================================================

    /**
     * Résout un numéro de séquence en garantissant l'unicité en base.
     * En cas de collision (race condition), incrémente jusqu'à trouver un numéro libre.
     *
     * @param formatter   Fonction qui génère le numéro à partir du seq
     * @param existsCheck Fonction qui vérifie l'existence en base
     * @param baseCount   Compteur de base (pour le premier seq)
     */
    private String resolveUniqueNumber(
            LongFunction<String> formatter,
            Predicate<String> existsCheck,
            long baseCount) {

        long seq    = baseCount + 1;
        String number = formatter.apply(seq);

        int maxAttempts = 20;
        int attempt     = 0;

        while (existsCheck.test(number) && attempt < maxAttempts) {
            attempt++;
            seq++;
            number = formatter.apply(seq);
        }

        if (existsCheck.test(number)) {
            throw new RuntimeException(
                    "Impossible de générer un numéro unique après " + maxAttempts + " tentatives. " +
                            "Dernier numéro essayé: " + number
            );
        }

        log.debug("Numéro unique généré: {}", number);
        return number;
    }

    // =========================================================================
    // MÉTHODES UTILITAIRES
    // =========================================================================

    protected String getReceiptPrefix(ReceiptType type) {
        return switch (type) {
            case SALE             -> "RCP";  // Ticket de vente
            case REFUND           -> "RFD";  // Ticket de remboursement
            case CANCELLATION     -> "CNL";  // Ticket d'annulation
            case SHIFT_OPENING    -> "OPN";  // Ouverture de caisse
            case SHIFT_CLOSING    -> "CLS";  // Fermeture de caisse
            case CASH_IN          -> "CIN";  // Entrée d'argent
            case CASH_OUT         -> "COT";  // Sortie d'argent
            case PAYMENT_RECEIVED -> "PMT";  // Réception paiement crédit
            case DELIVERY_NOTE    -> "BLD";  // Bon de livraison (ticket)
            case VOID             -> "VD";   // Ticket annulé
        };
    }

    public String generateDeliveryNoteNumber(InvoiceType type)  {
        return getPrefix(type);
    }

    public String generateCreditNoteNumber(InvoiceType type) {
        return getPrefix(type);
    }

    private String getPrefix(InvoiceType type) {
        String    prefix    = getInvoicePrefix(type);
        LocalDate today     = LocalDate.now();
        String    yearMonth = String.format("%04d%02d", today.getYear(), today.getMonthValue());

        long count = invoiceRepository.countByTypeAndYearMonth(
                type, today.getYear(), today.getMonthValue()
        );

        return resolveUniqueNumber(
                (seq) -> String.format("%s-%s-%04d", prefix, yearMonth, seq),
                invoiceRepository::existsByInvoiceNumber,
                count
        );
    }

    public String generateProformaNumber(InvoiceType type) {
        return getPrefix(type);
    }

    public String generateRefundNumber( ) {
        return String.format("RFD-%s-%s",
                LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }
    private String getReceiptPrefix(UUID storeId, ReceiptType type) {
        String    prefix    = getReceiptPrefix(type);
        String    storeCode = buildStoreCode(storeId);
        LocalDate today     = LocalDate.now();

        String dateStr = String.format("%04d%02d%02d",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth());

        long count = receiptRepository.countByStoreAndYearMonth(
                storeId, today.getYear(), today.getMonthValue()
        );

        return resolveUniqueNumber(
                (seq) -> String.format("%s-%s-%s-%04d", prefix, storeCode, dateStr, seq),
                receiptRepository::existsByReceiptNumber,
                count
        );
    }


    public String generateTransactionNumber() {
        return String.format("TX-%s-%s",
                LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE),
                UUID.randomUUID().toString().substring(0, 8).toUpperCase()
        );
    }
}
