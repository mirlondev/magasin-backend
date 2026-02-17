package org.odema.posnew.design.builder.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.OrderItem;
import org.odema.posnew.entity.Payment;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * Builder pour tickets de caisse format imprimante thermique (80mm / 3 pouces)
 * Format optimisé: 227 points (80mm) x hauteur dynamique
 */
@Slf4j
@Component
public class ReceiptDocumentBuilder extends AbstractPdfDocumentBuilder {

    // ✅ Format standard imprimante thermique 80mm
    private static final float THERMAL_WIDTH = 227f;  // 80mm en points
    private static final float THERMAL_MARGIN = 5f;
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // ✅ Valeurs par défaut hardcodées (fallback si @Value non injecté)
    private String companyName = "MIKILI POS";
    private String companyAddress = "123 Rue Principale";
    private String companyPhone = "+237 6XX XX XX XX";
    private String companyEmail = "contact@odema.com";
    private String companyTaxId = "TAX-123456";
    private String footerMessage = "Merci de votre visite !";

    // ✅ Setters pour injection manuelle si besoin
    public void setCompanyName(String companyName) {
        if (companyName != null && !companyName.isBlank()) {
            this.companyName = companyName;
        }
    }

    public void setCompanyAddress(String companyAddress) {
        if (companyAddress != null) this.companyAddress = companyAddress;
    }

    public void setCompanyPhone(String companyPhone) {
        if (companyPhone != null) this.companyPhone = companyPhone;
    }

    public void setCompanyTaxId(String companyTaxId) {
        if (companyTaxId != null) this.companyTaxId = companyTaxId;
    }

    public void setFooterMessage(String footerMessage) {
        if (footerMessage != null) this.footerMessage = footerMessage;
    }

    public ReceiptDocumentBuilder() {
        super(null);
    }

    public ReceiptDocumentBuilder(Order order) {
        super(order);
    }

    /**
     * ✅ Méthode pour configurer le builder avec les valeurs Spring
     * À appeler après création si les @Value ne sont pas injectées automatiquement
     */
    public ReceiptDocumentBuilder withConfig(String companyName, String companyAddress,
                                             String companyPhone, String companyTaxId,
                                             String footerMessage) {
        setCompanyName(companyName);
        setCompanyAddress(companyAddress);
        setCompanyPhone(companyPhone);
        setCompanyTaxId(companyTaxId);
        setFooterMessage(footerMessage);
        return this;
    }

    @Override
    public DocumentBuilder initialize() {
        Rectangle thermalSize = new Rectangle(THERMAL_WIDTH, PageSize.A4.getHeight());
        this.document = new Document(thermalSize, THERMAL_MARGIN, THERMAL_MARGIN,
                THERMAL_MARGIN, THERMAL_MARGIN);
        this.outputStream = new java.io.ByteArrayOutputStream();

        try {
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, outputStream);
            document.open();
            log.debug("Document PDF thermal initialisé (format 80mm)");
        } catch (DocumentException e) {
            log.error("Erreur initialisation PDF thermal", e);
            throw new RuntimeException("Erreur initialisation document", e);
        }
        return this;
    }

    @Override
    public DocumentBuilder addHeader() {
        try {
            // ✅ Null-safe avec fallback
            String safeCompanyName = companyName != null ? companyName : "ODEMA POS";

            Font titleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Paragraph title = new Paragraph(safeCompanyName.toUpperCase(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(2);
            document.add(title);

            Font infoFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
            Paragraph info = new Paragraph();
            info.setAlignment(Element.ALIGN_CENTER);
            info.setLeading(9);

            if (companyAddress != null && !companyAddress.isEmpty() &&
                    !companyAddress.equals("123 Rue Principale")) {
                info.add(new Chunk(companyAddress + "\n", infoFont));
            }
            if (companyPhone != null && !companyPhone.isEmpty() &&
                    !companyPhone.equals("+237 6XX XX XX XX")) {
                info.add(new Chunk("Tél: " + companyPhone + "\n", infoFont));
            }
            if (companyTaxId != null && !companyTaxId.isEmpty() &&
                    !companyTaxId.equals("TAX-123456")) {
                info.add(new Chunk("NIF: " + companyTaxId + "\n", infoFont));
            }

            document.add(info);
            addSeparatorLine();

            log.debug("En-tête thermal ajouté");

        } catch (DocumentException e) {
            log.error("Erreur ajout en-tête thermal", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public DocumentBuilder addMainInfo() {
        try {
            Font labelFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.DARK_GRAY);

            Paragraph docType = new Paragraph("TICKET DE CAISSE", titleFont);
            docType.setAlignment(Element.ALIGN_CENTER);
            docType.setSpacingAfter(5);
            document.add(docType);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{1f, 2f});
            infoTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            infoTable.getDefaultCell().setPadding(1);

            // ✅ Utiliser le numéro de ticket si disponible, sinon orderNumber
            String ticketNumber = order.getOrderNumber();
//            if (order.getReceipts() != null && !order.getReceipts().isEmpty()) {
//                ticketNumber = order.getReceipts().get(0).getReceiptNumber();
//            }

            addInfoRow(infoTable, "Ticket:", ticketNumber, labelFont, valueFont);
            addInfoRow(infoTable, "Date:",
                    order.getCreatedAt() != null ? order.getCreatedAt().format(DATETIME_FORMATTER) : "N/A",
                    labelFont, valueFont);
            addInfoRow(infoTable, "Caisse:",
                    order.getStore() != null ? order.getStore().getName() : "N/A",
                    labelFont, valueFont);
            addInfoRow(infoTable, "Caissier:",
                    order.getCashier() != null ? order.getCashier().getUsername() : "N/A",
                    labelFont, valueFont);

            if (order.getCustomer() != null) {
                addInfoRow(infoTable, "Client:",
                        order.getCustomer().getFullName() != null ? order.getCustomer().getFullName() : "N/A",
                        labelFont, valueFont);
                if (order.getCustomer().getPhone() != null) {
                    addInfoRow(infoTable, "Tél client:", order.getCustomer().getPhone(),
                            labelFont, valueFont);
                }
            }

            document.add(infoTable);
            addSeparatorLine();

            log.debug("Informations thermal ajoutées");
        } catch (DocumentException e) {
            log.error("Erreur ajout infos thermal", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public DocumentBuilder addItemsTable() {
        try {
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.WHITE);
            Font itemFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
            Font priceFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2.5f, 0.8f, 1.2f, 1.5f});

            BaseColor headerBg = new BaseColor(80, 80, 80);

            addHeaderCell(table, "Article", headerFont, headerBg);
            addHeaderCell(table, "Qté", headerFont, headerBg);
            addHeaderCell(table, "P.U.", headerFont, headerBg);
            addHeaderCell(table, "Total", headerFont, headerBg);

            for (OrderItem item : order.getItems()) {
                PdfPCell nameCell = getPdfPCell(item, itemFont);
                table.addCell(nameCell);

                PdfPCell qtyCell = new PdfPCell(new Phrase(
                        String.valueOf(item.getQuantity()), itemFont));
                qtyCell.setBorder(Rectangle.NO_BORDER);
                qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                qtyCell.setPadding(2);
                table.addCell(qtyCell);

                PdfPCell priceCell = new PdfPCell(new Phrase(
                        formatCurrency(item.getUnitPrice()), priceFont));
                priceCell.setBorder(Rectangle.NO_BORDER);
                priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                priceCell.setPadding(2);
                table.addCell(priceCell);

                PdfPCell totalCell = new PdfPCell(new Phrase(
                        formatCurrency(item.getFinalPrice()), priceFont));
                totalCell.setBorder(Rectangle.NO_BORDER);
                totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totalCell.setPadding(2);
                table.addCell(totalCell);
            }

            document.add(table);
            addSeparatorLine();

            log.debug("Articles thermal ajoutés ({} items)", order.getItems().size());
        } catch (DocumentException e) {
            log.error("Erreur ajout articles thermal", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    private static @NonNull PdfPCell getPdfPCell(OrderItem item, Font itemFont) {
        String articleName = item.getProduct() != null ?
                item.getProduct().getName() : "Article inconnu";

        if (articleName.length() > 22) {
            articleName = articleName.substring(0, 20) + "..";
        }

        String articleDetail = articleName;

        PdfPCell nameCell = new PdfPCell(new Phrase(articleDetail, itemFont));
        nameCell.setBorder(Rectangle.NO_BORDER);
        nameCell.setPadding(2);
        nameCell.setLeading(8, 8);
        return nameCell;
    }

    @Override
    public DocumentBuilder addTotals() {
        try {
            Font labelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
            Font totalFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            Font smallFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 1f});
            table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            table.getDefaultCell().setPadding(2);

            if (order.getSubtotal() != null) {
                addTotalRow(table, "Sous-total HT:", formatCurrency(order.getSubtotal()),
                        labelFont, valueFont);
            }

            if (order.getTaxAmount() != null && order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                String taxLabel = String.format("TVA (%s%%):",
                        order.getTaxRate() != null ? order.getTaxRate().toString() : "18");
                addTotalRow(table, taxLabel, formatCurrency(order.getTaxAmount()),
                        labelFont, valueFont);
            }

            if (order.getDiscountAmount() != null &&
                    order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(table, "Remise:", "-" + formatCurrency(order.getDiscountAmount()),
                        new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD,
                                new BaseColor(192, 57, 43)), valueFont);
            }

            PdfPCell sepCell = new PdfPCell();
            sepCell.setColspan(2);
            sepCell.setBorder(Rectangle.TOP);
            sepCell.setBorderWidth(1);
            sepCell.setPadding(3);
            table.addCell(sepCell);

            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL TTC", totalFont));
            totalLabelCell.setBorder(Rectangle.NO_BORDER);
            totalLabelCell.setPadding(3);
            table.addCell(totalLabelCell);

            PdfPCell totalValueCell = new PdfPCell(new Phrase(
                    formatCurrency(order.getTotalAmount()), totalFont));
            totalValueCell.setBorder(Rectangle.NO_BORDER);
            totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValueCell.setPadding(3);
            table.addCell(totalValueCell);

            // Paiements détaillés
            BigDecimal totalPaid = order.getTotalPaid();
            if (totalPaid != null && totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                addSeparatorLine();

                for (Payment payment : order.getPayments()) {
                    if (payment.getStatus() != null &&
                            (payment.getStatus().toString().equals("PAID") ||
                                    payment.getStatus().toString().equals("COMPLETED"))) {
                        String methodLabel = formatPaymentMethod(payment.getMethod());
                        addTotalRow(table, methodLabel + ":",
                                formatCurrency(payment.getAmount()), smallFont, valueFont);
                    }
                }

                if (order.getChangeAmount() != null &&
                        order.getChangeAmount().compareTo(BigDecimal.ZERO) > 0) {
                    addTotalRow(table, "Monnaie rendue:",
                            formatCurrency(order.getChangeAmount()), labelFont,
                            new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD,
                                    new BaseColor(39, 174, 96)));
                }

                BigDecimal remaining = order.getTotalAmount().subtract(totalPaid);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    addTotalRow(table, "Reste à payer:", formatCurrency(remaining),
                            new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.RED),
                            new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.RED));
                }
            }

            document.add(table);
            addSeparatorLine();

            log.debug("Totaux thermal ajoutés");
        } catch (DocumentException e) {
            log.error("Erreur ajout totaux thermal", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public DocumentBuilder addFooter() {
        try {
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC,
                    BaseColor.DARK_GRAY);
            Font smallFont = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL,
                    BaseColor.GRAY);
            Font barcodeFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);

            String safeFooter = footerMessage != null ? footerMessage : "Merci de votre visite !";
            Paragraph thanks = new Paragraph(safeFooter, footerFont);
            thanks.setAlignment(Element.ALIGN_CENTER);
            thanks.setSpacingBefore(5);
            thanks.setSpacingAfter(3);
            document.add(thanks);

            if (order.getOrderNumber() != null) {
                Paragraph barcode = new Paragraph("* " + order.getOrderNumber() + " *",
                        barcodeFont);
                barcode.setAlignment(Element.ALIGN_CENTER);
                barcode.setSpacingAfter(2);
                document.add(barcode);
            }

            Paragraph legal = new Paragraph(
                    "Conservez ce ticket pour tout échange sous 7 jours\n" +
                            "TVA incluse dans le prix - Merci de votre confiance", smallFont);
            legal.setAlignment(Element.ALIGN_CENTER);
            legal.setLeading(8);
            document.add(legal);

            document.add(Chunk.NEWLINE);
            document.add(Chunk.NEWLINE);

            Paragraph cutLine = new Paragraph("✂---------------------------✂",
                    new Font(Font.FontFamily.ZAPFDINGBATS, 8));
            cutLine.setAlignment(Element.ALIGN_CENTER);
            document.add(cutLine);

            log.debug("Pied de page thermal ajouté");
        } catch (DocumentException e) {
            log.error("Erreur ajout pied de page thermal", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private void addSeparatorLine() throws DocumentException {
        LineSeparator sep = new LineSeparator();
        sep.setLineColor(BaseColor.LIGHT_GRAY);
        sep.setLineWidth(0.5f);
        sep.setPercentage(100);
        document.add(new Chunk(sep));
        document.add(Chunk.NEWLINE);
    }

    private void addInfoRow(PdfPTable table, String label, String value,
                            Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(1);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "", valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(1);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(3);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value,
                             Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        labelCell.setPadding(2);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(2);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private String formatPaymentMethod(PaymentMethod method) {
        if (method == null) return "Espèces";
        return switch (method) {
            case CASH -> "Espèces";
            case CREDIT_CARD -> "Carte bancaire";
            case MOBILE_MONEY -> "Mobile Money";
            case CREDIT -> "Crédit";
            case CHECK -> "Chèque";
            case BANK_TRANSFER -> "Virement";
            default -> method.name();
        };
    }
}