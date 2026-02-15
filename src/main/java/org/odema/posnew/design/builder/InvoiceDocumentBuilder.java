package org.odema.posnew.design.builder;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InvoiceDocumentBuilder extends AbstractPdfDocumentBuilder {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public InvoiceDocumentBuilder() {
        super(null);
    }

    public InvoiceDocumentBuilder(Order order) {
        super(order);
    }

    @Override
    public DocumentBuilder addHeader() {
        try {
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1, 1});

            // Colonne gauche: Logo + Nom
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);

            Font companyFont = new Font(Font.FontFamily.HELVETICA, 18,
                    Font.BOLD, HEADER_COLOR);
            Paragraph company = new Paragraph(companyName, companyFont);
            leftCell.addElement(company);

            Font sloganFont = new Font(Font.FontFamily.HELVETICA, 9,
                    Font.ITALIC, TEXT_DARK);
            Paragraph slogan = new Paragraph(
                    "Votre partenaire commercial de confiance", sloganFont
            );
            leftCell.addElement(slogan);

            headerTable.addCell(leftCell);

            // Colonne droite: Coordonnées
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Font contactFont = new Font(Font.FontFamily.HELVETICA, 9,
                    Font.NORMAL, TEXT_DARK);

            Paragraph contacts = new Paragraph();
            contacts.setAlignment(Element.ALIGN_RIGHT);
            contacts.add(new Chunk(companyAddress + "\n", contactFont));
            contacts.add(new Chunk("Tél: " + companyPhone + "\n", contactFont));
            contacts.add(new Chunk("Email: " + companyEmail + "\n", contactFont));
            contacts.add(new Chunk("NIF: " + companyTaxId, contactFont));

            rightCell.addElement(contacts);
            headerTable.addCell(rightCell);

            document.add(headerTable);

            // Ligne de séparation
            LineSeparator separator = new LineSeparator();
            separator.setLineColor(HEADER_COLOR);
            separator.setLineWidth(2);
            document.add(new Chunk(separator));
            document.add(Chunk.NEWLINE);

            log.debug("En-tête facture ajouté");

        } catch (DocumentException e) {
            log.error("Erreur ajout en-tête", e);
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public DocumentBuilder addMainInfo() {
        try {
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1, 1});
            infoTable.setSpacingBefore(10f);
            infoTable.setSpacingAfter(15f);

            Font labelFont = new Font(Font.FontFamily.HELVETICA, 9,
                    Font.BOLD, TEXT_DARK);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);

            // Colonne gauche: Client
            PdfPCell clientCell = new PdfPCell();
            clientCell.setBorder(Rectangle.BOX);
            clientCell.setBorderColor(BaseColor.LIGHT_GRAY);
            clientCell.setPadding(10);
            clientCell.setBackgroundColor(new BaseColor(236, 240, 241));

            Paragraph clientTitle = new Paragraph("FACTURER À", labelFont);
            clientCell.addElement(clientTitle);
            clientCell.addElement(Chunk.NEWLINE);

            Customer customer = order.getCustomer();
            Paragraph clientInfo = new Paragraph();
            clientInfo.add(new Chunk(customer.getFullName() + "\n",
                    new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));

            if (customer.getAddress() != null) {
                clientInfo.add(new Chunk(customer.getAddress() + "\n", valueFont));
            }
            clientInfo.add(new Chunk("Tél: " + customer.getPhone() + "\n", valueFont));
            clientInfo.add(new Chunk(customer.getEmail(), valueFont));

            clientCell.addElement(clientInfo);
            infoTable.addCell(clientCell);

            // Colonne droite: Détails facture
            PdfPCell invoiceCell = new PdfPCell();
            invoiceCell.setBorder(Rectangle.BOX);
            invoiceCell.setBorderColor(HEADER_COLOR);
            invoiceCell.setBorderWidth(2);
            invoiceCell.setPadding(10);

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20,
                    Font.BOLD, HEADER_COLOR);
            Paragraph title = new Paragraph("FACTURE", titleFont);
            invoiceCell.addElement(title);
            invoiceCell.addElement(Chunk.NEWLINE);

            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);

            addDetailRow(detailsTable, "N° Facture:", order.getOrderNumber(),
                    labelFont, valueFont);
            addDetailRow(detailsTable, "Date:",
                    order.getCreatedAt().format(DATE_FORMATTER),
                    labelFont, valueFont);

            invoiceCell.addElement(detailsTable);
            infoTable.addCell(invoiceCell);

            document.add(infoTable);

            log.debug("Informations principales ajoutées");

        } catch (DocumentException e) {
            log.error("Erreur ajout infos principales", e);
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public DocumentBuilder addItemsTable() {
        try {
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 9,
                    Font.BOLD, BaseColor.WHITE);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 9);

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4, 1.5f, 2, 1.5f, 2});
            table.setSpacingBefore(10f);

            // En-têtes
            String[] headers = {"DESCRIPTION", "QTÉ", "PRIX UNIT.", "REMISE", "TOTAL"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(HEADER_COLOR);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(8);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            // Lignes articles
            boolean alternate = false;
            for (OrderItem item : order.getItems()) {
                BaseColor rowColor = alternate
                        ? new BaseColor(236, 240, 241) : BaseColor.WHITE;

                table.addCell(createStyledCell(
                        item.getProduct().getName(),
                        cellFont, Element.ALIGN_LEFT, rowColor
                ));
                table.addCell(createStyledCell(
                        item.getQuantity().toString(),
                        cellFont, Element.ALIGN_CENTER, rowColor
                ));
                table.addCell(createStyledCell(
                        formatCurrency(item.getUnitPrice()),
                        cellFont, Element.ALIGN_RIGHT, rowColor
                ));
                table.addCell(createStyledCell(
                        formatCurrency(item.getDiscountAmount()),
                        cellFont, Element.ALIGN_RIGHT, rowColor
                ));
                table.addCell(createStyledCell(
                        formatCurrency(item.getFinalPrice()),
                        cellFont, Element.ALIGN_RIGHT, rowColor
                ));

                alternate = !alternate;
            }

            document.add(table);

            log.debug("Tableau articles ajouté");

        } catch (DocumentException e) {
            log.error("Erreur ajout tableau articles", e);
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public DocumentBuilder addTotals() {
        try {
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(40);
            table.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.setSpacingBefore(15f);

            Font labelFont = new Font(Font.FontFamily.HELVETICA, 10,
                    Font.NORMAL, TEXT_DARK);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);
            Font totalFont = new Font(Font.FontFamily.HELVETICA, 14,
                    Font.BOLD, HEADER_COLOR);

            // Sous-total
            addTotalRow(table, "Sous-total:",
                    formatCurrency(order.getSubtotal()),
                    labelFont, valueFont);

            // TVA si applicable
            if (order.getTaxAmount() != null &&
                    order.getTaxAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                addTotalRow(table, "TVA:",
                        formatCurrency(order.getTaxAmount()),
                        labelFont, valueFont);
            }

            // Remise si applicable
            if (order.getDiscountAmount() != null &&
                    order.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                addTotalRow(table, "Remise:",
                        formatCurrency(order.getDiscountAmount()),
                        labelFont, valueFont);
            }

            // Total avec fond coloré
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL", totalFont));
            totalLabelCell.setBorder(Rectangle.NO_BORDER);
            totalLabelCell.setBackgroundColor(new BaseColor(236, 240, 241));
            totalLabelCell.setPadding(8);
            table.addCell(totalLabelCell);

            PdfPCell totalValueCell = new PdfPCell(
                    new Phrase(formatCurrency(order.getTotalAmount()), totalFont)
            );
            totalValueCell.setBorder(Rectangle.NO_BORDER);
            totalValueCell.setBackgroundColor(new BaseColor(236, 240, 241));
            totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValueCell.setPadding(8);
            table.addCell(totalValueCell);

            // Montant payé
            if (order.getAmountPaid() != null &&
                    order.getAmountPaid().compareTo(java.math.BigDecimal.ZERO) > 0) {
                addTotalRow(table, "Montant payé:",
                        formatCurrency(order.getAmountPaid()),
                        labelFont, valueFont);
            }

            // Solde dû
            java.math.BigDecimal amountDue = order.getTotalAmount()
                    .subtract(order.getAmountPaid() != null
                            ? order.getAmountPaid()
                            : java.math.BigDecimal.ZERO);

            if (amountDue.compareTo(java.math.BigDecimal.ZERO) > 0) {
                Font dueFont = new Font(Font.FontFamily.HELVETICA, 12,
                        Font.BOLD, new BaseColor(231, 76, 60));
                addTotalRow(table, "Solde dû:",
                        formatCurrency(amountDue),
                        labelFont, dueFont);
            }

            document.add(table);

            log.debug("Totaux ajoutés");

        } catch (DocumentException e) {
            log.error("Erreur ajout totaux", e);
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public DocumentBuilder addFooter() {
        try {
            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            // Notes
            if (order.getNotes() != null && !order.getNotes().isEmpty()) {
                Font notesFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC);
                Paragraph notes = new Paragraph();
                notes.add(new Chunk("Notes: ",
                        new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD)));
                notes.add(new Chunk(order.getNotes(), notesFont));
                document.add(notes);
                document.add(Chunk.NEWLINE);
            }

            // Ligne séparation
            LineSeparator separator = new LineSeparator();
            separator.setLineColor(BaseColor.LIGHT_GRAY);
            document.add(new Chunk(separator));
            document.add(Chunk.NEWLINE);

            // Conditions de paiement
            Font termsFont = new Font(Font.FontFamily.HELVETICA, 8,
                    Font.NORMAL, TEXT_DARK);
            Paragraph terms = new Paragraph();
            terms.add(new Chunk("CONDITIONS DE PAIEMENT\n",
                    new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD)));
            terms.add(new Chunk(
                    "• Paiement à réception ou selon échéance\n" +
                            "• Retard: pénalités de 10% après échéance\n" +
                            "• Tribunal de commerce de Pointe-Noire\n",
                    termsFont
            ));
            document.add(terms);

            document.add(Chunk.NEWLINE);

            // Remerciements
            Font thanksFont = new Font(Font.FontFamily.HELVETICA, 10,
                    Font.ITALIC, HEADER_COLOR);
            Paragraph thanks = new Paragraph("Merci pour votre confiance !", thanksFont);
            thanks.setAlignment(Element.ALIGN_CENTER);
            document.add(thanks);

            log.debug("Pied de page ajouté");

        } catch (DocumentException e) {
            log.error("Erreur ajout pied de page", e);
            throw new RuntimeException(e);
        }

        return this;
    }

    // Méthodes utilitaires
    private void addDetailRow(PdfPTable table, String label, String value,
                              Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    private void addTotalRow(PdfPTable table, String label, String value,
                             Font labelFont, Font valueFont) {
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
}
