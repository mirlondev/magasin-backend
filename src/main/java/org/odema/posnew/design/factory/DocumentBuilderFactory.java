package org.odema.posnew.design.builder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.impl.*;
import org.odema.posnew.design.context.DocumentBuildContext;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.Receipt;
import org.odema.posnew.entity.Refund;
import org.odema.posnew.entity.ShiftReport;
import org.odema.posnew.entity.enums.DocumentType;
import org.odema.posnew.entity.enums.OrderType;
import org.odema.posnew.entity.enums.ReceiptType;
import org.odema.posnew.exception.BadRequestException;
import org.springframework.stereotype.Component;

/**
 * Factory pour la création des builders de documents.
 * Centralise l'instanciation et la configuration des builders selon le type de document.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentBuilderFactory {

    // Configuration de l'entreprise (injectée depuis application.properties)
    private final DocumentBuilderConfig config;

    /**
     * Crée un builder pour un ticket de caisse standard (vente POS)
     */
    public ReceiptDocumentBuilder createReceiptBuilder(Order order) {
        log.debug("Création ReceiptDocumentBuilder pour commande {}", order.getOrderNumber());
        return new ReceiptDocumentBuilder(order)
                .withConfig(
                        config.getCompanyName(),
                        config.getCompanyAddress(),
                        config.getCompanyPhone(),
                        config.getCompanyTaxId(),
                        config.getReceiptFooterMessage()
                );
    }

    /**
     * Crée un builder pour un ticket d'événement de caisse
     */
    public ShiftReceiptDocumentBuilder createShiftReceiptBuilder(Receipt receipt) {
        log.debug("Création ShiftReceiptDocumentBuilder pour receipt {}", receipt.getReceiptNumber());
        return new ShiftReceiptDocumentBuilder(receipt)
                .withConfig(
                        config.getCompanyName(),
                        config.getCompanyAddress(),
                        config.getCompanyPhone(),
                        config.getCompanyTaxId()
                );
    }

    /**
     * Crée un builder pour une facture
     */
    public InvoiceDocumentBuilder createInvoiceBuilder(Order order) {
        log.debug("Création InvoiceDocumentBuilder pour commande {}", order.getOrderNumber());
        return new InvoiceDocumentBuilder(order)
                .withConfig(
                        config.getCompanyName(),
                        config.getCompanyAddress(),
                        config.getCompanyPhone(),
                        config.getCompanyEmail(),
                        config.getCompanyTaxId(),
                        config.getCompanyBankAccount(),
                        config.getCompanyBankName(),
                        config.getCompanyWebsite()
                );
    }

    /**
     * Crée un builder pour un proforma (devis)
     */
    public ProformaDocumentBuilder createProformaBuilder(Order order) {
        log.debug("Création ProformaDocumentBuilder pour commande {}", order.getOrderNumber());
        return new ProformaDocumentBuilder(order)
                .withConfig(
                        config.getCompanyName(),
                        config.getCompanyAddress(),
                        config.getCompanyPhone(),
                        config.getCompanyEmail(),
                        config.getCompanyTaxId(),
                        config.getCompanyBankAccount(),
                        config.getCompanyBankName(),
                        config.getCompanyWebsite()
                )
                .withValidityDays(30);
    }

    /**
     * Crée un builder pour un bon de livraison
     */
    public DeliveryNoteDocumentBuilder createDeliveryNoteBuilder(Order order) {
        log.debug("Création DeliveryNoteDocumentBuilder pour commande {}", order.getOrderNumber());
        return new DeliveryNoteDocumentBuilder(order)
                .withConfig(
                        config.getCompanyName(),
                        config.getCompanyAddress(),
                        config.getCompanyPhone(),
                        config.getCompanyEmail(),
                        config.getCompanyTaxId(),
                        config.getCompanyWebsite()
                );
    }

    /**
     * Crée un builder pour un ticket de remboursement
     */
    public RefundDocumentBuilder createRefundBuilder(Refund refund, Order originalOrder) {
        log.debug("Création RefundDocumentBuilder pour remboursement {}", refund.getRefundNumber());
        return new RefundDocumentBuilder(refund, originalOrder)
                .withConfig(
                        config.getCompanyName(),
                        config.getCompanyAddress(),
                        config.getCompanyPhone(),
                        config.getCompanyTaxId(),
                        config.getRefundFooterMessage()
                );
    }

    /**
     * Crée un builder pour un ticket d'annulation
     */
    public CancellationDocumentBuilder createCancellationBuilder(Order order, String reason, String cancelledBy) {
        log.debug("Création CancellationDocumentBuilder pour commande {}", order.getOrderNumber());
        return new CancellationDocumentBuilder(order)
                .withConfig(
                        config.getCompanyName(),
                        config.getCompanyAddress(),
                        config.getCompanyPhone(),
                        config.getCompanyTaxId(),
                        config.getCancellationFooterMessage()
                )
                .withCancellationReason(reason)
                .withCancelledBy(cancelledBy);
    }

    /**
     * Crée un builder pour une note de crédit (avoir)
     */
    public CreditNoteDocumentBuilder createCreditNoteBuilder(Order order, Refund refund) {
        log.debug("Création CreditNoteDocumentBuilder pour avoir sur commande {}", order.getOrderNumber());
        return new CreditNoteDocumentBuilder(order)
                .withConfig(
                        config.getCompanyName(),
                        config.getCompanyAddress(),
                        config.getCompanyPhone(),
                        config.getCompanyEmail(),
                        config.getCompanyTaxId(),
                        config.getCompanyWebsite()
                )
                .withRefund(refund)
                .withReason(refund.getReason());
    }

    /**
     * Crée un builder pour un rapport X (intermédiaire)
     */
    public XReportDocumentBuilder createXReportBuilder(ShiftReport shiftReport) {
        log.debug("Création XReportDocumentBuilder pour session {}", shiftReport.getShiftReportId());
        return new XReportDocumentBuilder()
                .withConfig(
                        config.getCompanyName(),
                        config.getCompanyAddress(),
                        config.getCompanyPhone()
                )
                .withShiftReport(shiftReport);
    }

    /**
     * Crée un builder pour un rapport Z (fin de journée)
     */
    public ZReportDocumentBuilder createZReportBuilder(ShiftReport shiftReport) {
        log.debug("Création ZReportDocumentBuilder pour session {}", shiftReport.getShiftReportId());
        return new ZReportDocumentBuilder()
                .withConfig(
                        config.getCompanyName(),
                        config.getCompanyAddress(),
                        config.getCompanyPhone(),
                        config.getCompanyTaxId()
                )
                .withShiftReport(shiftReport);
    }

    /**
     * Détermine le type de document approprié pour une commande
     */
    public DocumentType determineDocumentType(Order order) {
        if (order == null || order.getOrderType() == null) {
            throw new BadRequestException("Impossible de déterminer le type de document");
        }

        return switch (order.getOrderType()) {
            case POS_SALE, ONLINE -> DocumentType.TICKET;
            case CREDIT_SALE -> DocumentType.INVOICE;
            case PROFORMA -> DocumentType.PROFORMA;
            case RETURN -> DocumentType.REFUND;
            case EXCHANGE -> DocumentType.CREDIT_NOTE;
            default -> throw new BadRequestException("Type de commande non supporté: " + order.getOrderType());
        };
    }

    /**
     * Détermine le type de receipt approprié pour une opération
     */
    public ReceiptType determineReceiptType(String operation) {
        if (operation == null) {
            return ReceiptType.SALE;
        }

        return switch (operation.toUpperCase()) {
            case "OPENING" -> ReceiptType.SHIFT_OPENING;
            case "CLOSING" -> ReceiptType.SHIFT_CLOSING;
            case "CASH_IN" -> ReceiptType.CASH_IN;
            case "CASH_OUT" -> ReceiptType.CASH_OUT;
            case "REFUND" -> ReceiptType.REFUND;
            case "CANCEL" -> ReceiptType.CANCELLATION;
            case "DELIVERY" -> ReceiptType.DELIVERY_NOTE;
            default -> ReceiptType.SALE;
        };
    }

    /**
     * Crée le builder approprié selon le type de document demandé
     */
//    public DocumentBuilder createBuilder(DocumentType type, Order order) {
//        log.debug("Création builder pour type {} et commande {}", type, order.getOrderNumber());
//
//        return switch (type) {
//            case TICKET, RECEIPT-> createReceiptBuilder(order);
//            case INVOICE -> createInvoiceBuilder(order);
//            case PROFORMA -> createProformaBuilder(order);
//            case DELIVERY_NOTE -> createDeliveryNoteBuilder(order);
//
//            default -> throw new BadRequestException("Type de document non supporté pour cette opération: " + type);
//        };
//    }

    /**
     * Version enrichie avec DocumentBuildContext pour les cas complexes
     */
    public DocumentBuilder createBuilder(DocumentType type, DocumentBuildContext ctx) {
        log.debug("Création builder type {} pour commande {}", type, ctx.getOrder().getOrderNumber());

        return switch (type) {
            case TICKET ->
                    createReceiptBuilder(ctx.getOrder());

            case INVOICE ->
                    createInvoiceBuilder(ctx.getOrder());

            case PROFORMA ->
                    createProformaBuilder(ctx.getOrder());

            case DELIVERY_NOTE ->
                    createDeliveryNoteBuilder(ctx.getOrder());

            case REFUND -> {
                if (ctx.getRefund() == null)
                    throw new BadRequestException("Refund requis pour DocumentType.REFUND");
                yield createRefundBuilder(ctx.getRefund(), ctx.getOrder());
            }

            case CANCELLATION -> {
                if (ctx.getCancellationReason() == null || ctx.getCancelledBy() == null)
                    throw new BadRequestException("Raison et auteur requis pour DocumentType.CANCELLATION");
                yield createCancellationBuilder(
                        ctx.getOrder(),
                        ctx.getCancellationReason(),
                        ctx.getCancelledBy()
                );
            }

            case CREDIT_NOTE -> {
                if (ctx.getRefund() == null)
                    throw new BadRequestException("Refund requis pour DocumentType.CREDIT_NOTE");
                yield createCreditNoteBuilder(ctx.getOrder(), ctx.getRefund());
            }

            case VOID ->
                // VOID réutilise le builder d'annulation sans raison obligatoire
                    createCancellationBuilder(ctx.getOrder(),
                            ctx.getCancellationReason() != null ? ctx.getCancellationReason() : "Annulé",
                            ctx.getCancelledBy()        != null ? ctx.getCancelledBy()        : "SYSTEM");

            case SHIFT_REPORT ->
                    throw new BadRequestException(
                            "SHIFT_REPORT ne passe pas par DocumentBuilderFactory — " +
                                    "utiliser createXReportBuilder() ou createZReportBuilder() directement"
                    );

            case RECEIPT ->
                    createReceiptBuilder(ctx.getOrder());
        };
    }

    /**
     * Ancienne signature conservée pour compatibilité — délègue vers la nouvelle
     */
    public DocumentBuilder createBuilder(DocumentType type, Order order) {
        return createBuilder(type, DocumentBuildContext.forOrder(order));
    }

}
