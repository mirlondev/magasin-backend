package org.odema.posnew.design.builder.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.entity.Customer;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.OrderItem;
import org.odema.posnew.entity.Payment;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder pour factures professionnelles A4
 * Design moderne avec couleurs, logos, QR codes, et tous les champs légaux
 */
@Slf4j
@Component
public class InvoiceDocumentBuilder extends AbstractPdfDocumentBuilder {

    // Couleurs professionnelles
    private static final BaseColor PRIMARY_COLOR = new BaseColor(41, 128, 185);    // Bleu professionnel
    private static final BaseColor SECONDARY_COLOR = new BaseColor(52, 152, 219);  // Bleu clair
    private static final BaseColor ACCENT_COLOR = new BaseColor(231, 76, 60);      // Rouge pour alertes
    private static final BaseColor SUCCESS_COLOR = new BaseColor(39, 174, 96);    // Vert pour succès
    private static final BaseColor TEXT_DARK = new BaseColor(44, 62, 80);         // Texte principal
    private static final BaseColor TEXT_LIGHT = new BaseColor(127, 140, 141);     // Texte secondaire
    private static final BaseColor BG_LIGHT = new BaseColor(236, 240, 241);       // Fond gris clair
    private static final BaseColor WHITE = BaseColor.WHITE;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Valeurs configurables avec fallback
    private String companyName = "ODEMA POS";
    private String companyAddress = "123 Rue Principale, Pointe-Noire";
    private String companyPhone = "+237 6XX XX XX XX";
    private String companyEmail = "contact@odema.com";
    private String companyTaxId = "TAX-123456789";
    private String companyBankAccount = "IBAN: CM21 1234 5678 9012 3456 7890 123";
    private String companyBankName = "Banque Atlantique";
    private String companyWebsite = "www.odema.com";
    private String companyLogoPath = "static/logo.png";

    public InvoiceDocumentBuilder() {
        super(null);
    }

    public InvoiceDocumentBuilder(Order order) {
        super(order);
    }

    public InvoiceDocumentBuilder withConfig(String companyName, String companyAddress,
                                             String companyPhone, String companyEmail,
                                             String companyTaxId, String companyBankAccount,
                                             String companyBankName, String companyWebsite) {
        if (companyName != null) this.companyName = companyName;
        if (companyAddress != null) this.companyAddress = companyAddress;
        if (companyPhone != null) this.companyPhone = companyPhone;
        if (companyEmail != null) this.companyEmail = companyEmail;
        if (companyTaxId != null) this.companyTaxId = companyTaxId;
        if (companyBankAccount != null) this.companyBankAccount = companyBankAccount;
        if (companyBankName != null) this.companyBankName = companyBankName;
        if (companyWebsite != null) this.companyWebsite = companyWebsite;
        return this;
    }

    @Override
    public DocumentBuilder initialize() {
        this.document = new Document(PageSize.A4, 36, 36, 36, 36); // Marges 0.5 inch
        this.outputStream = new java.io.ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();
            log.debug("Document facture A4 initialisé");
        } catch (DocumentException e) {
            log.error("Erreur initialisation facture", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public DocumentBuilder addHeader() {
        try {
            // Tableau principal header
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1.2f, 1f});
            headerTable.setSpacingAfter(20);

            // === COLONNE GAUCHE: Logo et entreprise ===
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setPadding(5);

            // Logo (si disponible)
            addLogo(leftCell);

            // Nom entreprise stylisé
            Font companyFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, PRIMARY_COLOR);
            Paragraph company = new Paragraph(companyName.toUpperCase(), companyFont);
            company.setSpacingAfter(5);
            leftCell.addElement(company);

            // Slogan
            Font sloganFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, TEXT_LIGHT);
            Paragraph slogan = new Paragraph("Votre partenaire commercial de confiance", sloganFont);
            slogan.setSpacingAfter(10);
            leftCell.addElement(slogan);

            // Coordonnées entreprise
            Font contactFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_DARK);
            Paragraph contacts = new Paragraph();
            contacts.setLeading(12);
            contacts.add(new Chunk("📍 " + companyAddress + "\n", contactFont));
            contacts.add(new Chunk("📞 " + companyPhone + "\n", contactFont));
            contacts.add(new Chunk("✉ " + companyEmail + "\n", contactFont));
            contacts.add(new Chunk("🌐 " + companyWebsite + "\n", contactFont));
            contacts.add(new Chunk("NIF: " + companyTaxId, new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, TEXT_DARK)));
            leftCell.addElement(contacts);

            headerTable.addCell(leftCell);

            // === COLONNE DROITE: Type document et infos légales ===
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.setPadding(5);

            // Badge FACTURE stylisé
            PdfPTable badgeTable = new PdfPTable(1);
            badgeTable.setWidthPercentage(60);
            badgeTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell badgeCell = new PdfPCell(new Phrase("FACTURE",
                    new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD, WHITE)));
            badgeCell.setBackgroundColor(PRIMARY_COLOR);
            badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            badgeCell.setPadding(15);
            badgeCell.setBorder(Rectangle.NO_BORDER);
            badgeCell.setCellEvent(new RoundRectangle(8));
            badgeTable.addCell(badgeCell);

            rightCell.addElement(badgeTable);

            // Numéro et date
            Font infoLabelFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_LIGHT);
            Font infoValueFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, TEXT_DARK);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingBefore(15);
            infoTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            addInfoPair(infoTable, "N° Facture:", order.getOrderNumber(), infoLabelFont, infoValueFont);
            addInfoPair(infoTable, "Date d'émission:",
                    LocalDateTime.now().format(DATE_FORMATTER), infoLabelFont, infoValueFont);
            addInfoPair(infoTable, "Date échéance:",
                    calculateDueDate().format(DATE_FORMATTER), infoLabelFont, infoValueFont);
            addInfoPair(infoTable, "Référence:",
                    "REF-" + order.getOrderNumber().substring(4), infoLabelFont, infoValueFont);

            rightCell.addElement(infoTable);
            headerTable.addCell(rightCell);

            document.add(headerTable);

            // Ligne de séparation décorative
            addDecorativeLine();

            log.debug("En-tête facture ajouté");

        } catch (DocumentException e) {
            log.error("Erreur en-tête facture", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public DocumentBuilder addMainInfo() {
        try {
            PdfPTable mainTable = new PdfPTable(2);
            mainTable.setWidthPercentage(100);
            mainTable.setWidths(new float[]{1f, 1f});
            mainTable.setSpacingBefore(10);
            mainTable.setSpacingAfter(20);

            // === CLIENT (Facturer à) ===
            PdfPCell clientCell = createStyledBox("FACTURER À", PRIMARY_COLOR);

            Customer customer = order.getCustomer();
            if (customer != null) {
                Font nameFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, TEXT_DARK);
                Font infoFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_DARK);

                Paragraph clientInfo = new Paragraph();
                clientInfo.setLeading(14);
                clientInfo.add(new Chunk(customer.getFullName() + "\n", nameFont));

                if (customer.getLastName() != null && !customer.getFirstName().isEmpty()) {
                    clientInfo.add(new Chunk(customer.getFullName() + "\n",
                            new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, TEXT_LIGHT)));
                }

                if (customer.getAddress() != null) {
                    clientInfo.add(new Chunk(customer.getAddress() + "\n", infoFont));
                }

                clientInfo.add(new Chunk("📞 " + (customer.getPhone() != null ? customer.getPhone() : "N/A") + "\n", infoFont));

                if (customer.getEmail() != null) {
                    clientInfo.add(new Chunk("✉ " + customer.getEmail() + "\n", infoFont));
                }

                clientCell.addElement(clientInfo);
            }

            mainTable.addCell(clientCell);

            // === LIVRAISON / EXPÉDITION ===
            PdfPCell shippingCell = createStyledBox("LIVRAISON / EXPÉDITION", SECONDARY_COLOR);

            Font infoFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_DARK);
            Paragraph shippingInfo = new Paragraph();
            shippingInfo.setLeading(14);
            shippingInfo.add(new Chunk("📦 Mode: " + getShippingMethod() + "\n", infoFont));
            shippingInfo.add(new Chunk("🏪 Magasin: " +
                    (order.getStore() != null ? order.getStore().getName() : "N/A") + "\n", infoFont));
            shippingInfo.add(new Chunk("👤 Vendeur: " +
                    (order.getCashier() != null ? order.getCashier().getFullName() : "N/A") + "\n", infoFont));
            shippingInfo.add(new Chunk("📅 Commande: " +
                    order.getCreatedAt().format(DATETIME_FORMATTER), infoFont));

            shippingCell.addElement(shippingInfo);
            mainTable.addCell(shippingCell);

            document.add(mainTable);

            log.debug("Infos principales facture ajoutées");

        } catch (DocumentException e) {
            log.error("Erreur infos facture", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public DocumentBuilder addItemsTable() {
        try {
            // Titre section
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR);
            Paragraph sectionTitle = new Paragraph("DÉTAIL DES ARTICLES", sectionFont);
            sectionTitle.setSpacingAfter(10);
            document.add(sectionTitle);

            // Tableau des articles
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.5f, 3.5f, 1f, 1.5f, 1.5f, 1.5f});
            table.setSpacingBefore(5);
            table.setSpacingAfter(15);

            // En-têtes avec style
            String[] headers = {"N°", "DESCRIPTION", "QTÉ", "P.U. HT", "REMISE", "TOTAL TTC"};
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, WHITE);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(PRIMARY_COLOR);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(8);
                cell.setBorder(Rectangle.NO_BORDER);
                table.addCell(cell);
            }

            // Lignes articles
            int index = 1;
            boolean alternate = false;

            for (OrderItem item : order.getItems()) {
                BaseColor rowColor = alternate ? BG_LIGHT : WHITE;
                Font cellFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_DARK);
                Font descFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_DARK);

                // N°
                table.addCell(createCell(String.valueOf(index++), cellFont,
                        Element.ALIGN_CENTER, rowColor));

                // Description détaillée
                StringBuilder desc = new StringBuilder();
                desc.append(item.getProduct() != null ? item.getProduct().getName() : "Article");
                if (item.getProduct() != null && item.getProduct().getSku() != null) {
                    desc.append("\nRéf: ").append(item.getProduct().getSku());
                }
                if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                    desc.append("\nNote: ").append(item.getNotes());
                }

                PdfPCell descCell = createCell(desc.toString(), descFont,
                        Element.ALIGN_LEFT, rowColor);
                descCell.setLeading(12, 12);
                table.addCell(descCell);

                // Qté
                table.addCell(createCell(String.valueOf(item.getQuantity()), cellFont,
                        Element.ALIGN_CENTER, rowColor));

                // Prix unitaire HT
                BigDecimal unitPriceHT = calculateHT(item.getUnitPrice());
                table.addCell(createCell(formatCurrency(unitPriceHT), cellFont,
                        Element.ALIGN_RIGHT, rowColor));

                // Remise
                String discountStr = "-";
                table.addCell(createCell(discountStr, cellFont, Element.ALIGN_CENTER, rowColor));

                // Total TTC ligne
                table.addCell(createCell(formatCurrency(item.getFinalPrice()),
                        new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, TEXT_DARK),
                        Element.ALIGN_RIGHT, rowColor));

                alternate = !alternate;
            }

            document.add(table);

            // Résumé quantités
            Font summaryFont = new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, TEXT_LIGHT);
            Paragraph summary = new Paragraph(
                    String.format("Total articles: %d | Total quantités: %d",
                            order.getItems().size(),
                            order.getItems().stream().mapToInt(OrderItem::getQuantity).sum()),
                    summaryFont);
            summary.setAlignment(Element.ALIGN_RIGHT);
            summary.setSpacingAfter(10);
            document.add(summary);

            log.debug("Tableau articles facture ajouté");

        } catch (DocumentException e) {
            log.error("Erreur tableau articles", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public DocumentBuilder addTotals() {
        try {
            PdfPTable mainTable = new PdfPTable(2);
            mainTable.setWidthPercentage(100);
            mainTable.setWidths(new float[]{1.5f, 1f});
            mainTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            // === COLONNE GAUCHE: Récapitulatif TVA et paiements ===
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);

            // Tableau détail TVA
            Font sectionFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, TEXT_DARK);
            Paragraph tvaTitle = new Paragraph("DÉTAIL DE LA TVA", sectionFont);
            tvaTitle.setSpacingAfter(8);
            leftCell.addElement(tvaTitle);

            PdfPTable tvaTable = new PdfPTable(3);
            tvaTable.setWidthPercentage(80);
            tvaTable.setWidths(new float[]{1f, 1f, 1f});

            addTvaHeader(tvaTable, "Base HT", "Taux", "Montant TVA");

            // Ligne TVA normale
            BigDecimal tauxTVA = order.getTaxRate() != null ? order.getTaxRate() : new BigDecimal("18.00");
            addTvaRow(tvaTable, order.getSubtotal(), tauxTVA + "%", order.getTaxAmount());

            // ✅ CORRECTION: Add the table directly to leftCell, not wrapped in a PdfPCell
            leftCell.addElement(tvaTable);

            // Add spacing after TVA table
            leftCell.addElement(new Paragraph(" "));

            // Informations bancaires
            Paragraph bankTitle = new Paragraph("COORDONNÉES BANCAIRES", sectionFont);
            bankTitle.setSpacingBefore(15);
            bankTitle.setSpacingAfter(8);
            leftCell.addElement(bankTitle);

            Font bankFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_DARK);
            Paragraph bankInfo = new Paragraph();
            bankInfo.setLeading(12);
            bankInfo.add(new Chunk("🏦 " + companyBankName + "\n", bankFont));
            bankInfo.add(new Chunk(companyBankAccount + "\n", bankFont));
            bankInfo.add(new Chunk("Bénéficiaire: " + companyName,
                    new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, TEXT_DARK)));
            leftCell.addElement(bankInfo);

            // Méthodes de paiement acceptées
            Paragraph methodsTitle = new Paragraph("PAIEMENT ACCEPTÉ", sectionFont);
            methodsTitle.setSpacingBefore(10);
            methodsTitle.setSpacingAfter(5);
            leftCell.addElement(methodsTitle);

            Paragraph methods = new Paragraph("💳 Carte bancaire  |  📱 Mobile Money  |  🏦 Virement  |  💵 Espèces",
                    new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, TEXT_LIGHT));
            leftCell.addElement(methods);

            mainTable.addCell(leftCell);

            // === COLONNE DROITE: Totaux ===
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(100);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            Font labelFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_LIGHT);
            Font valueFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_DARK);

            // Sous-total HT
            addTotalRow(totalsTable, "Total HT:", formatCurrency(order.getSubtotal()), labelFont, valueFont);

            // Remises
            if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "Remise commerciale:",
                        "-" + formatCurrency(order.getDiscountAmount()),
                        labelFont, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, ACCENT_COLOR));
            }

            // TVA
            if (order.getTaxAmount() != null && order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                String tvaLabel = String.format("TVA (%s%%):",
                        order.getTaxRate() != null ? order.getTaxRate().toString() : "18");
                addTotalRow(totalsTable, tvaLabel, formatCurrency(order.getTaxAmount()), labelFont, valueFont);
            }

            // Ligne de séparation propre
            PdfPCell separatorCell = new PdfPCell();
            separatorCell.setColspan(2);
            separatorCell.setBorder(Rectangle.TOP);
            separatorCell.setBorderWidth(2);
            separatorCell.setBorderColor(PRIMARY_COLOR);
            separatorCell.setPadding(5);
            totalsTable.addCell(separatorCell);

            // TOTAL TTC (en grand)
            Font totalLabelFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR);
            Font totalValueFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD, PRIMARY_COLOR);

            PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL TTC", totalLabelFont));
            totalLabel.setBorder(Rectangle.NO_BORDER);
            totalLabel.setPadding(8);
            totalsTable.addCell(totalLabel);

            PdfPCell totalValue = new PdfPCell(new Phrase(formatCurrency(order.getTotalAmount()), totalValueFont));
            totalValue.setBorder(Rectangle.NO_BORDER);
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalValue.setPadding(8);
            totalsTable.addCell(totalValue);

            // Montant en lettres
            PdfPCell wordsCell = new PdfPCell(new Phrase(
                    "Arrêté la présente facture à la somme de:\n" +
                            amountInWords(order.getTotalAmount()) + " FCFA",
                    new Font(Font.FontFamily.HELVETICA, 9, Font.ITALIC, TEXT_LIGHT)));
            wordsCell.setColspan(2);
            wordsCell.setBorder(Rectangle.NO_BORDER);
            wordsCell.setPadding(5);
            totalsTable.addCell(wordsCell);

            // État du paiement
            List<Payment> validPayments = order.getPayments().stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PAID)
                    .collect(Collectors.toList());

            BigDecimal totalPaid = validPayments.stream()
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "Déjà payé:", formatCurrency(totalPaid),
                        labelFont, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, SUCCESS_COLOR));

                BigDecimal remaining = order.getTotalAmount().subtract(totalPaid);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    addTotalRow(totalsTable, "Reste à payer:", formatCurrency(remaining),
                            labelFont, new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, ACCENT_COLOR));
                } else if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                    addTotalRow(totalsTable, "Monnaie rendue:", formatCurrency(remaining.abs()),
                            labelFont, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, SUCCESS_COLOR));
                } else {
                    PdfPCell paidCell = new PdfPCell(new Phrase("✓ PAYÉ EN TOTALITÉ",
                            new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, SUCCESS_COLOR)));
                    paidCell.setColspan(2);
                    paidCell.setBorder(Rectangle.NO_BORDER);
                    paidCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    paidCell.setPadding(5);
                    totalsTable.addCell(paidCell);
                }
            } else {
                PdfPCell unpaidCell = new PdfPCell(new Phrase("⚠ NON PAYÉ",
                        new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, ACCENT_COLOR)));
                unpaidCell.setColspan(2);
                unpaidCell.setBorder(Rectangle.NO_BORDER);
                unpaidCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                unpaidCell.setPadding(5);
                totalsTable.addCell(unpaidCell);
            }

            rightCell.addElement(totalsTable);
            mainTable.addCell(rightCell);

            document.add(mainTable);

            log.debug("Totaux facture ajoutés");

        } catch (DocumentException e) {
            log.error("Erreur totaux facture", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public DocumentBuilder addFooter() {
        try {
            // Ligne de séparation
            addDecorativeLine();

            // Tableau conditions et signature
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{1.5f, 1f});
            footerTable.setSpacingBefore(10);

            // Conditions de vente
            PdfPCell termsCell = new PdfPCell();
            termsCell.setBorder(Rectangle.NO_BORDER);

            Font termsTitleFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, TEXT_DARK);
            Font termsFont = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, TEXT_LIGHT);

            Paragraph terms = new Paragraph();
            terms.add(new Chunk("CONDITIONS GÉNÉRALES DE VENTE\n", termsTitleFont));
            terms.add(new Chunk(
                    "• Paiement comptant à réception de la facture\n" +
                            "• Escompte 2% pour paiement anticipé sous 8 jours\n" +
                            "• Pénalités de retard: 3 fois le taux d'intérêt légal\n" +
                            "• Retention de titre: marchandises livrées restent notre propriété jusqu'au paiement intégral\n" +
                            "• Tribunal compétent: Commerce de Pointe-Noire\n" +
                            "• Conforme à la loi OHADA sur la sécurité des transactions électroniques",
                    termsFont));
            termsCell.addElement(terms);
            footerTable.addCell(termsCell);

            // Zone signature
            PdfPCell signCell = getPdfPCell();

            footerTable.addCell(signCell);
            document.add(footerTable);

            // Remerciement stylisé
            document.add(Chunk.NEWLINE);
            Paragraph thanks = new Paragraph("Merci pour votre confiance et votre fidélité !",
                    new Font(Font.FontFamily.HELVETICA, 12, Font.ITALIC, PRIMARY_COLOR));
            thanks.setAlignment(Element.ALIGN_CENTER);
            document.add(thanks);

            // QR Code et infos légales bottom
            PdfPTable bottomTable = new PdfPTable(2);
            bottomTable.setWidthPercentage(100);
            bottomTable.setSpacingBefore(15);

            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.NO_BORDER);
            // TODO: Ajouter QR code ici si besoin
            Paragraph qrText = new Paragraph("[QR Code de vérification]",
                    new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, TEXT_LIGHT));
            qrCell.addElement(qrText);
            bottomTable.addCell(qrCell);

            PdfPCell legalCell = new PdfPCell();
            legalCell.setBorder(Rectangle.NO_BORDER);
            legalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Font tinyFont = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, TEXT_LIGHT);
            Paragraph legal = new Paragraph();
            legal.setAlignment(Element.ALIGN_RIGHT);
            legal.add(new Chunk("Document généré électroniquement - Valide sans signature\n", tinyFont));
            legal.add(new Chunk("Société enregistrée au RCCM de Pointe-Noire\n", tinyFont));
            legal.add(new Chunk("© " + java.time.Year.now() + " " + companyName + " - Tous droits réservés", tinyFont));
            legalCell.addElement(legal);
            bottomTable.addCell(legalCell);

            document.add(bottomTable);

            log.debug("Pied de page facture ajouté");

        } catch (DocumentException e) {
            log.error("Erreur footer facture", e);
            throw new RuntimeException(e);
        }
        return this;
    }

    private @NonNull PdfPCell getPdfPCell() {
        PdfPCell signCell = new PdfPCell();
        signCell.setBorder(Rectangle.NO_BORDER);
        signCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph signTitle = new Paragraph("POUR " + companyName.toUpperCase(),
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, TEXT_DARK));
        signTitle.setAlignment(Element.ALIGN_CENTER);
        signTitle.setSpacingAfter(30);
        signCell.addElement(signTitle);

        Paragraph signature = new Paragraph("Signature et cachet",
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, TEXT_LIGHT));
        signature.setAlignment(Element.ALIGN_CENTER);
        signCell.addElement(signature);
        return signCell;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private void addLogo(PdfPCell cell) {
        try {
            java.io.InputStream logoStream = getClass().getClassLoader()
                    .getResourceAsStream(companyLogoPath);
            if (logoStream != null) {
                Image logo = Image.getInstance(logoStream.readAllBytes());
                logo.scaleToFit(80, 60);
                logo.setAlignment(Element.ALIGN_LEFT);
                cell.addElement(logo);
                cell.addElement(Chunk.NEWLINE);
            }
        } catch (Exception e) {
            log.debug("Logo non trouvé: {}", companyLogoPath);
        }
    }

    private void addDecorativeLine() throws DocumentException {
        LineSeparator sep = new LineSeparator();
        sep.setLineColor(PRIMARY_COLOR);
        sep.setLineWidth(2);
        document.add(new Chunk(sep));
        document.add(Chunk.NEWLINE);
    }

    private PdfPCell createStyledBox(String title, BaseColor color) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(color);
        cell.setBorderWidth(2);
        cell.setPadding(10);
        cell.setBackgroundColor(BG_LIGHT);

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, color);
        Paragraph titlePara = new Paragraph(title, titleFont);
        titlePara.setSpacingAfter(8);
        cell.addElement(titlePara);

        return cell;
    }

    private PdfPCell createCell(String text, Font font, int alignment, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    private void addInfoPair(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(3);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(3);
        table.addCell(valueCell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
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

    private void addTvaHeader(PdfPTable table, String... headers) {
        Font font = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, WHITE);
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(SECONDARY_COLOR);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }

    private void addTvaRow(PdfPTable table, BigDecimal base, String taux, BigDecimal montant) {
        Font font = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_DARK);
        table.addCell(createCell(formatCurrency(base), font, Element.ALIGN_RIGHT, WHITE));
        table.addCell(createCell(taux, font, Element.ALIGN_CENTER, WHITE));
        table.addCell(createCell(formatCurrency(montant), font, Element.ALIGN_RIGHT, WHITE));
    }

    protected String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.2f FCFA", amount);
    }

    private BigDecimal calculateHT(BigDecimal ttc) {
        if (ttc == null) return BigDecimal.ZERO;
        // Approximation: HT = TTC / 1.18 (pour 18% TVA)
        return ttc.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
    }

    private LocalDateTime calculateDueDate() {
        // Par défaut: 30 jours
        return LocalDateTime.now().plusDays(30);
    }

    private String getShippingMethod() {
        // À adapter selon votre logique
        return "Retrait en magasin";
    }

    /**
     * Convertit un montant en lettres (simplifié pour FCFA)
     */
    private String amountInWords(BigDecimal amount) {
        // TODO: Implémenter conversion complète si nécessaire
        // Pour l'instant, retourne le montant en chiffres avec indication
        return String.format("%s (en chiffres)", formatCurrency(amount));
    }

    // Classe interne pour coins arrondis
    private static class RoundRectangle implements PdfPCellEvent {
        private final float radius;

        RoundRectangle(float radius) {
            this.radius = radius;
        }

        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte cb = canvases[PdfPTable.BACKGROUNDCANVAS];
            cb.roundRectangle(
                    position.getLeft() + 2,
                    position.getBottom() + 2,
                    position.getWidth() - 4,
                    position.getHeight() - 4,
                    radius
            );
            cb.stroke();
        }
    }
}