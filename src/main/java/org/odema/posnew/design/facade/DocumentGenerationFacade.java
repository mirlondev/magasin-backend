package org.odema.posnew.design.facade;

import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.dto.response.RefundResponse;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.Refund;
import org.odema.posnew.entity.enums.DocumentType;
import org.odema.posnew.entity.enums.InvoiceType;
import org.odema.posnew.entity.enums.ReceiptType;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.service.InvoiceService;
import org.odema.posnew.service.ReceiptService;
import org.odema.posnew.service.RefundService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentGenerationFacade {

    private final InvoiceService invoiceService;
    private final ReceiptService receiptService;
    private final RefundService  refundService;   // à créer si absent

    // ═══════════════════════════════════════════════════
    // POINT D'ENTRÉE PRINCIPAL — DocumentType
    // ═══════════════════════════════════════════════════

    public byte[] generateDocument(Order order, DocumentType documentType) {
        log.info("Génération document {} pour commande {}",
                documentType, order.getOrderNumber());

        return switch (documentType) {

            // ── Vente standard ───────────────────────────────────
            case TICKET, RECEIPT ->
                    generateReceiptPdf(order, ReceiptType.SALE);

            // ── Factures ─────────────────────────────────────────
            case INVOICE ->
                    generateInvoicePdf(order, InvoiceType.INVOICE);

            case PROFORMA ->
                    generateInvoicePdf(order, InvoiceType.PROFORMA);

            // ── Retours / avoirs ─────────────────────────────────
            case REFUND ->
                    generateRefundPdf(order);

            case CREDIT_NOTE ->
                    generateInvoicePdf(order, InvoiceType.CREDIT_NOTE);

            // ── Logistique ───────────────────────────────────────
            case DELIVERY_NOTE ->
                    generateInvoicePdf(order, InvoiceType.DELIVERY_NOTE);

            // ── Caisse ───────────────────────────────────────────
            case CANCELLATION ->
                    generateReceiptPdf(order, ReceiptType.CANCELLATION);

            case VOID ->
                    generateReceiptPdf(order, ReceiptType.VOID);

            // SHIFT_REPORT n'est pas lié à une Order — passer par ShiftReportService
            case SHIFT_REPORT ->
                    throw new BadRequestException(
                            "SHIFT_REPORT doit être généré via ShiftReportService, pas depuis une commande"
                    );
        };
    }

    // ═══════════════════════════════════════════════════
    // POINT D'ENTRÉE PAR ReceiptType
    // (ouverture/fermeture caisse, cash in/out, etc.)
    // ═══════════════════════════════════════════════════

    public byte[] generateReceiptByType(UUID orderId, ReceiptType type) {
        log.info("Génération ticket type {} pour commande {}", type, orderId);

        return switch (type) {
            case SALE, CANCELLATION, VOID, REFUND,
                 PAYMENT_RECEIVED, DELIVERY_NOTE ->
                    generateReceiptPdfById(orderId, type);

            // Les tickets shift sont liés à une ShiftReport, pas à une Order
            case SHIFT_OPENING, SHIFT_CLOSING, CASH_IN, CASH_OUT ->
                    throw new BadRequestException(
                            "Le type " + type + " doit être généré via ReceiptService.generateShift*()"
                    );
        };
    }

    // ═══════════════════════════════════════════════════
    // POINT D'ENTRÉE PAR InvoiceType
    // ═══════════════════════════════════════════════════

    public byte[] generateInvoiceByType(UUID orderId, InvoiceType type) {
        log.info("Génération facture type {} pour commande {}", type, orderId);

        return switch (type) {
            case INVOICE           -> getInvoicePdf(orderId);
            case CREDIT_SALE       -> getInvoicePdf(orderId);   // même builder, type différent
            case PROFORMA          -> getProformaPdf(orderId);
            case CREDIT_NOTE       -> getCreditNotePdf(orderId);
            case DELIVERY_NOTE     -> getDeliveryNotePdf(orderId);
            case PURCHASE_ORDER    -> getPurchaseOrderPdf(orderId);
            case QUOTE             -> getQuotePdf(orderId);
            case INVOICE_CORRECTED -> getCorrectedInvoicePdf(orderId);
        };
    }

    // ═══════════════════════════════════════════════════
    // MÉTHODES PRIVÉES — Receipt
    // ═══════════════════════════════════════════════════

    private byte[] generateReceiptPdf(Order order, ReceiptType type) {
        try {
            var response = receiptService.generateReceipt(order.getOrderId(), type);
            return receiptService.generateReceiptPdf(UUID.fromString(response.receiptId()));
        } catch (Exception e) {
            log.error("Erreur génération ticket {} pour commande {}",
                    type, order.getOrderNumber(), e);
            throw new BadRequestException("Erreur génération ticket: " + e.getMessage());
        }
    }

    private byte[] generateReceiptPdfById(UUID orderId, ReceiptType type) {
        try {
            var response = receiptService.generateReceipt(orderId, type);
            return receiptService.generateReceiptPdf(UUID.fromString(response.receiptId()));
        } catch (Exception e) {
            log.error("Erreur génération ticket {} pour commande {}", type, orderId, e);
            throw new BadRequestException("Erreur génération ticket: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════
    // MÉTHODES PRIVÉES — Invoice
    // ═══════════════════════════════════════════════════

    private byte[] generateInvoicePdf(Order order, InvoiceType type) {
        try {
            invoiceService.generateInvoice(order.getOrderId());
            return invoiceService.getOrGenerateInvoicePdf(order.getOrderId());
        } catch (Exception e) {
            log.error("Erreur génération facture {} pour commande {}",
                    type, order.getOrderNumber(), e);
            throw new BadRequestException("Erreur génération facture: " + e.getMessage());
        }
    }

    private byte[] getInvoicePdf(UUID orderId) {
        try {
            return invoiceService.getOrGenerateInvoicePdf(orderId);
        } catch (IOException | DocumentException e) {
            throw new BadRequestException("Erreur récupération facture: " + e.getMessage());
        }
    }

    private byte[] getProformaPdf(UUID orderId) {
        try {
            return invoiceService.getOrGenerateProformaPdf(orderId);
        } catch (IOException | DocumentException e) {
            throw new BadRequestException("Erreur récupération proforma: " + e.getMessage());
        }
    }

    private byte[] getCreditNotePdf(UUID orderId) {
        try {
            return invoiceService.getOrGenerateCreditNotePdf(orderId);
        } catch (IOException | DocumentException e) {
            throw new BadRequestException("Erreur récupération avoir: " + e.getMessage());
        }
    }

    private byte[] getDeliveryNotePdf(UUID orderId) {
        try {
            return invoiceService.getOrGenerateDeliveryNotePdf(orderId);
        } catch (IOException | DocumentException e) {
            throw new BadRequestException("Erreur récupération bon de livraison: " + e.getMessage());
        }
    }

    private byte[] getPurchaseOrderPdf(UUID orderId) {
        try {
            return invoiceService.getOrGeneratePurchaseOrderPdf(orderId);
        } catch (IOException | DocumentException e) {
            throw new BadRequestException("Erreur récupération bon de commande: " + e.getMessage());
        }
    }

    private byte[] getQuotePdf(UUID orderId) {
        try {
            return invoiceService.getOrGenerateQuotePdf(orderId);
        } catch (IOException | DocumentException e) {
            throw new BadRequestException("Erreur récupération devis: " + e.getMessage());
        }
    }

    private byte[] getCorrectedInvoicePdf(UUID orderId) {
        try {
            return invoiceService.getOrGenerateCorrectedInvoicePdf(orderId);
        } catch (IOException | DocumentException e) {
            throw new BadRequestException("Erreur récupération facture rectificative: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════
    // MÉTHODES PRIVÉES — Refund
    // ═══════════════════════════════════════════════════

    private byte[] generateRefundPdf(Order order) {
        try {
            RefundResponse refund = refundService.getRefundByOrder(order.getOrderId());
            return refundService.generateRefundPdf(refund.refundId());
        } catch (Exception e) {
            log.error("Erreur génération remboursement pour commande {}",
                    order.getOrderNumber(), e);
            throw new BadRequestException("Erreur génération remboursement: " + e.getMessage());
        }
    }
}