package org.odema.posnew.design.builder.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.OrderItem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class ReceiptDocumentBuilder extends AbstractPdfDocumentBuilder {

    @Value("${app.company.name:ODEMA POS}")
    private String companyName;

    @Value("${app.company.address:123 Rue Principale, Ville}")
    private String companyAddress;

    @Value("${app.company.phone:+237 6XX XX XX XX}")
    private String companyPhone;

    @Value("${app.company.email:contact@odema.com}")
    private String companyEmail;

    @Value("${app.company.tax-id:TAX-123456}")
    private String companyTaxId;
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ReceiptDocumentBuilder() {
        super(null);
    }

    public ReceiptDocumentBuilder(Order order) {
        super(order);
    }

    @Override
    public DocumentBuilder addHeader() {
        try {
            // Style ticket de caisse (plus compact)
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Paragraph title = new Paragraph(companyName, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Font infoFont = new Font(Font.FontFamily.HELVETICA, 8);
            Paragraph info = new Paragraph();
            info.setAlignment(Element.ALIGN_CENTER);
            info.add(new Chunk(companyAddress + "\n", infoFont));
            info.add(new Chunk("Tel: " + companyPhone + "\n", infoFont));
            info.add(new Chunk("NIF: " + companyTaxId + "\n", infoFont));
            document.add(info);

            document.add(new Paragraph("================================"));
            document.add(Chunk.NEWLINE);

            log.debug("En-tête ticket ajouté");

        } catch (DocumentException e) {
            log.error("Erreur ajout en-tête ticket", e);
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public DocumentBuilder addMainInfo() {
        try {
            Font labelFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 8);

            Paragraph info = new Paragraph();
            info.add(new Chunk("TICKET DE CAISSE\n\n", labelFont));
            info.add(new Chunk("N°: ", labelFont));
            info.add(new Chunk(order.getOrderNumber() + "\n", valueFont));
            info.add(new Chunk("Date: ", labelFont));
            info.add(new Chunk(order.getCreatedAt().format(DATETIME_FORMATTER) + "\n",
                    valueFont));
            info.add(new Chunk("Caisse: ", labelFont));
            info.add(new Chunk(order.getStore().getName() + "\n", valueFont));
            info.add(new Chunk("Caissier: ", labelFont));
            info.add(new Chunk(order.getCashier().getUsername() + "\n", valueFont));

            if (order.getCustomer() != null) {
                info.add(new Chunk("Client: ", labelFont));
                info.add(new Chunk(order.getCustomer().getFullName() + "\n", valueFont));
            }

            document.add(info);
            document.add(new Paragraph("================================"));
            document.add(Chunk.NEWLINE);

            log.debug("Informations ticket ajoutées");

        } catch (DocumentException e) {
            log.error("Erreur ajout infos ticket", e);
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override
    public DocumentBuilder addItemsTable() {
        try {
            Font itemFont = new Font(Font.FontFamily.HELVETICA, 8);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3, 1, 2});

            // En-têtes simplifiés
            table.addCell(new Phrase("Article", boldFont));
            table.addCell(new Phrase("Qté", boldFont));
            table.addCell(new Phrase("Prix", boldFont));

            // Articles
            for (OrderItem item : order.getItems()) {
                table.addCell(new Phrase(item.getProduct().getName(), itemFont));
                table.addCell(new Phrase(item.getQuantity().toString(), itemFont));
                table.addCell(new Phrase(formatCurrency(item.getFinalPrice()), itemFont));
            }

            document.add(table);
            document.add(Chunk.NEWLINE);

            log.debug("Articles ticket ajoutés");

        } catch (DocumentException e) {
            log.error("Erreur ajout articles ticket", e);
            throw new RuntimeException(e);
        }

        return this;
    }

    @Override

    public DocumentBuilder addTotals() {
        try {
            document.add(new Paragraph("================================"));

            Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 9);
            Font totalFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);

            Paragraph totals = new Paragraph();
            totals.add(new Chunk("Sous-total: ", labelFont));
            totals.add(new Chunk(formatCurrency(order.getSubtotal()) + "\n", valueFont));

            if (order.getTaxAmount() != null &&
                    order.getTaxAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                totals.add(new Chunk("TVA: ", labelFont));
                totals.add(new Chunk(formatCurrency(order.getTaxAmount()) + "\n", valueFont));
            }

            if (order.getDiscountAmount() != null &&
                    order.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                totals.add(new Chunk("Remise: ", labelFont));
                totals.add(new Chunk(formatCurrency(order.getDiscountAmount()) + "\n", valueFont));
            }

            totals.add(Chunk.NEWLINE);
            totals.add(new Chunk("TOTAL: ", totalFont));
            totals.add(new Chunk(formatCurrency(order.getTotalAmount()) + "\n", totalFont));

            // ✅ FIXED: Get total paid from payments, not deprecated field
            BigDecimal totalPaid = order.getTotalPaid();
            if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                totals.add(Chunk.NEWLINE);
                totals.add(new Chunk("Payé: ", labelFont));
                totals.add(new Chunk(formatCurrency(totalPaid) + "\n", valueFont));

                // ✅ FIXED: Calculate change properly
                BigDecimal change = totalPaid.subtract(order.getTotalAmount());
                if (change.compareTo(BigDecimal.ZERO) > 0) {
                    totals.add(new Chunk("Rendu: ", labelFont));
                    totals.add(new Chunk(formatCurrency(change) + "\n", valueFont));
                }
            }

            document.add(totals);

            log.debug("Totaux ticket ajoutés");

        } catch (DocumentException e) {
            log.error("Erreur ajout totaux ticket", e);
            throw new RuntimeException(e);
        }

        return this;
    }


    @Override
    public DocumentBuilder addFooter() {
        try {
            document.add(new Paragraph("================================"));
            document.add(Chunk.NEWLINE);

            Font footerFont = new Font(Font.FontFamily.HELVETICA, 7, Font.ITALIC);
            Paragraph footer = new Paragraph();
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.add(new Chunk("Merci de votre visite !\n", footerFont));
            footer.add(new Chunk("À bientôt chez " + companyName, footerFont));

            document.add(footer);

            log.debug("Pied de page ticket ajouté");

        } catch (DocumentException e) {
            log.error("Erreur ajout pied de page ticket", e);
            throw new RuntimeException(e);
        }

        return this;
    }
}
