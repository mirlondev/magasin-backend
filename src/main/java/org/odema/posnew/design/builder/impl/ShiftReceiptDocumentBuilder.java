package org.odema.posnew.design.builder.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.entity.Receipt;
import org.odema.posnew.entity.enums.ReceiptType;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Builder Pattern - Construit les tickets pour événements de caisse:
 * - Ouverture / Fermeture de session
 * - Entrée / Sortie d'argent
 * - Remboursements
 * - Annulations
 */
@Slf4j
public class ShiftReceiptDocumentBuilder extends AbstractPdfDocumentBuilder {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final Receipt receipt;

    public ShiftReceiptDocumentBuilder(Receipt receipt) {
        super(null); // Pas de commande associée
        this.receipt = receipt;
    }

    @Override
    public org.odema.posnew.design.builder.DocumentBuilder addHeader() {
        try {
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font infoFont = new Font(Font.FontFamily.HELVETICA, 8);

            Paragraph title = new Paragraph(companyName, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph address = new Paragraph();
            address.setAlignment(Element.ALIGN_CENTER);
            address.add(new Chunk(companyAddress + "\n", infoFont));
            address.add(new Chunk("Tél: " + companyPhone + "\n", infoFont));
            address.add(new Chunk("NIF: " + companyTaxId, infoFont));
            document.add(address);

            document.add(separator());
            document.add(Chunk.NEWLINE);

        } catch (DocumentException e) {
            log.error("Erreur en-tête ticket caisse", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public org.odema.posnew.design.builder.DocumentBuilder addMainInfo() {
        try {
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 9);

            // Titre selon type
            String typeLabel = formatReceiptTypeLabel(receipt.getReceiptType());
            Paragraph typeTitle = new Paragraph(typeLabel, boldFont);
            typeTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(typeTitle);

            document.add(Chunk.NEWLINE);

            // Informations document
            PdfPTable info = new PdfPTable(2);
            info.setWidthPercentage(100);
            info.setWidths(new float[]{1.5f, 2f});

            addInfoRow(info, "N° Ticket:", receipt.getReceiptNumber(), boldFont, normalFont);
            addInfoRow(info, "Date:",
                    receipt.getReceiptDate().format(DT_FMT), boldFont, normalFont);
            addInfoRow(info, "Caissier:",
                    receipt.getCashier().getUsername(), boldFont, normalFont);
            addInfoRow(info, "Point de vente:",
                    receipt.getStore().getName(), boldFont, normalFont);

            if (receipt.getShiftReport() != null) {
                addInfoRow(info, "Session:",
                        receipt.getShiftReport().getShiftReportId().toString()
                                .substring(0, 8).toUpperCase(),
                        boldFont, normalFont);
            }

            document.add(info);
            document.add(Chunk.NEWLINE);

        } catch (DocumentException e) {
            log.error("Erreur info principale ticket caisse", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public org.odema.posnew.design.builder.DocumentBuilder addItemsTable() {
        // Pas d'articles pour les tickets de caisse
        // Les notes constituent le "contenu" de ce type de ticket
        try {
            if (receipt.getNotes() != null && !receipt.getNotes().isBlank()) {
                document.add(separator());
                Font notesFont = new Font(Font.FontFamily.HELVETICA, 9);
                Paragraph notes = new Paragraph(receipt.getNotes(), notesFont);
                document.add(notes);
                document.add(Chunk.NEWLINE);
            }
        } catch (DocumentException e) {
            log.error("Erreur notes ticket caisse", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public org.odema.posnew.design.builder.DocumentBuilder addTotals() {
        try {
            if (receipt.getTotalAmount() == null
                    || receipt.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
                return this;
            }

            document.add(separator());

            Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 9);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(60);
            table.setHorizontalAlignment(Element.ALIGN_RIGHT);

            // Montant principal
            String amountLabel = switch (receipt.getReceiptType()) {
                case SHIFT_OPENING -> "Fond initial:";
                case SHIFT_CLOSING -> "Total ventes:";
                case CASH_IN -> "Montant entré:";
                case CASH_OUT -> "Montant sorti:";
                case PAYMENT_RECEIVED -> "Paiement reçu:";
                default -> "MONTANT:";
            };

            addTotalRow(table, amountLabel,
                    formatAmount(receipt.getTotalAmount()),
                    boldFont, boldFont);

            document.add(table);
            document.add(Chunk.NEWLINE);

        } catch (DocumentException e) {
            log.error("Erreur totaux ticket caisse", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public org.odema.posnew.design.builder.DocumentBuilder addFooter() {
        try {
            document.add(separator());
            document.add(Chunk.NEWLINE);

            Font footerFont = new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC);
            Paragraph footer = new Paragraph();
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.add(new Chunk(companyName + " - Document de caisse\n", footerFont));
            footer.add(new Chunk("Ce document est généré automatiquement.", footerFont));
            document.add(footer);

        } catch (DocumentException e) {
            log.error("Erreur pied de page ticket caisse", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    // ========== MÉTHODES UTILITAIRES ==========

    private Chunk separator() {
        return new Chunk("================================\n",
                new Font(Font.FontFamily.COURIER, 9));
    }

    private void addInfoRow(PdfPTable table, String label, String value,
                            Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(3);
        table.addCell(valueCell);
    }

    private void addTotalRow(PdfPTable table, String label, String value,
                             Font labelFont, Font valueFont) {
        getPdfCell(table, label, value, labelFont, valueFont);
    }

    public static void getPdfCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.2f FCFA", amount);
    }

    private String formatReceiptTypeLabel(ReceiptType type) {
        return switch (type) {
            case SALE -> "TICKET DE CAISSE - VENTE";
            case REFUND -> "TICKET DE REMBOURSEMENT";
            case CANCELLATION -> "TICKET D'ANNULATION";
            case SHIFT_OPENING -> "OUVERTURE DE CAISSE";
            case SHIFT_CLOSING -> "FERMETURE DE CAISSE - JOURNAL";
            case CASH_IN -> "ENTRÉE D'ARGENT EN CAISSE";
            case CASH_OUT -> "SORTIE D'ARGENT DE CAISSE";
            case PAYMENT_RECEIVED -> "REÇU DE PAIEMENT";
            case DELIVERY_NOTE -> "BON DE LIVRAISON";
            case VOID -> "TICKET ANNULÉ";
        };
    }
}
