package org.odema.posnew.design.builder.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.entity.Customer;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.Refund;
import org.odema.posnew.entity.RefundItem;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Builder pour les notes de crédit (avoirs).
 * Format A4 professionnel - Document comptable avec valeur fiscale.
 */
@Slf4j
@Component
public class CreditNoteDocumentBuilder extends AbstractPdfDocumentBuilder {

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DT   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String companyName        = "ODEMA POS";
    private String companyAddress     = "123 Rue Principale, Pointe-Noire";
    private String companyPhone       = "+237 6XX XX XX XX";
    private String companyEmail       = "contact@odema.com";
    private String companyTaxId       = "TAX-123456789";
    private String companyWebsite     = "www.odema.com";
    private String logoPath           = "static/logo.png";

    private String creditNoteNumber;
    private String originalInvoiceNumber;
    private Refund refund;
    private String reason;
    private final StringBuilder html = new StringBuilder();

    public CreditNoteDocumentBuilder() { super(null); }

    public CreditNoteDocumentBuilder(Order order) {
        super(order);
    }

    public CreditNoteDocumentBuilder withConfig(
            String companyName, String companyAddress,
            String companyPhone, String companyEmail,
            String companyTaxId, String companyWebsite) {
        if (companyName    != null) this.companyName    = companyName;
        if (companyAddress != null) this.companyAddress = companyAddress;
        if (companyPhone   != null) this.companyPhone   = companyPhone;
        if (companyEmail   != null) this.companyEmail   = companyEmail;
        if (companyTaxId   != null) this.companyTaxId   = companyTaxId;
        if (companyWebsite != null) this.companyWebsite = companyWebsite;
        return this;
    }

    public CreditNoteDocumentBuilder withCreditNoteNumber(String number) {
        this.creditNoteNumber = number;
        return this;
    }

    public CreditNoteDocumentBuilder withOriginalInvoice(String invoiceNumber) {
        this.originalInvoiceNumber = invoiceNumber;
        return this;
    }

    public CreditNoteDocumentBuilder withRefund(Refund refund) {
        this.refund = refund;
        return this;
    }

    public CreditNoteDocumentBuilder withReason(String reason) {
        this.reason = reason;
        return this;
    }

    @Override
    public DocumentBuilder initialize() {
        this.outputStream = new ByteArrayOutputStream();
        html.setLength(0);
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" ")
                .append("\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">")
                .append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"fr\" lang=\"fr\">")
                .append("<head>")
                .append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>")
                .append("<style>").append(buildCss()).append("</style>")
                .append("</head><body>");
        return this;
    }

    @Override
    public DocumentBuilder addHeader() {
        String logoHtml = buildLogoTag();

        html.append("<table class=\"credit-header\"><tr>")
                .append("<td class=\"header-left\">")
                .append(logoHtml)
                .append("<div class=\"company-name\">").append(esc(companyName)).append("</div>")
                .append("<div class=\"company-sub\">").append(esc(companyAddress)).append("</div>")
                .append("<div class=\"company-contacts\">")
                .append(esc(companyPhone)).append(" &#183; ").append(esc(companyEmail))
                .append("<br/>NIF: ").append(esc(companyTaxId))
                .append(" &#183; ").append(esc(companyWebsite))
                .append("</div>")
                .append("</td>")
                .append("<td class=\"header-right\">")
                .append("<div class=\"credit-badge\">AVOIR</div>")
                .append("<div class=\"credit-subtitle\">NOTE DE CR&#201;DIT</div>")
                .append("<table class=\"meta-table\">")
                .append(metaRow("N&#176; Avoir", esc(creditNoteNumber != null ? creditNoteNumber : "AVO-" + System.currentTimeMillis())))
                .append(metaRow("Date", java.time.LocalDateTime.now().format(FMT_DATE)))
                .append(metaRow("Facture orig.", originalInvoiceNumber != null ? esc(originalInvoiceNumber) : "&#8212;"))
                .append(metaRow("Commande", order != null ? esc(order.getOrderNumber()) : "&#8212;"))
                .append("</table>")
                .append("</td>")
                .append("</tr></table>");

        return this;
    }

    @Override
    public DocumentBuilder addMainInfo() {
        Customer c = order != null ? order.getCustomer() : null;

        html.append("<table class=\"info-grid\"><tr>")

                // Client
                .append("<td class=\"info-box info-box--navy\">")
                .append("<div class=\"info-box__title\">CLIENT</div>")
                .append("<div class=\"info-box__name\">")
                .append(c != null ? esc(c.getFullName()) : "&#8212;")
                .append("</div>")
                .append("<div class=\"info-box__body\">");

        if (c != null && c.getAddress() != null) {
            html.append(esc(c.getAddress())).append("<br/>");
        }
        html.append("T&#233;l: ").append(c != null && c.getPhone() != null ? esc(c.getPhone()) : "&#8212;").append("<br/>");
        if (c != null && c.getEmail() != null) {
            html.append(esc(c.getEmail()));
        }

        html.append("</div>")
                .append("</td>")

                // Motif de l'avoir
                .append("<td class=\"info-box info-box--red\">")
                .append("<div class=\"info-box__title\">MOTIF DE L'AVOIR</div>")
                .append("<div class=\"info-box__body\">");

        if (reason != null && !reason.isBlank()) {
            html.append(esc(reason));
        } else if (refund != null && refund.getReason() != null) {
            html.append(esc(refund.getReason()));
        } else {
            html.append("Remboursement suite &#224; retour de marchandise");
        }

        html.append("</div>")
                .append("<div class=\"credit-status\">&#8617; MONTANT &#192; REMBOURSER</div>")
                .append("</td>")

                .append("</tr></table>");

        return this;
    }

    @Override
    public DocumentBuilder addItemsTable() {
        html.append("<div class=\"section-title\">ARTICLES CONCERN&#201;S</div>")
                .append("<table class=\"items-table\">")
                .append("<thead><tr>")
                .append("<th class=\"col-num\">N&#176;</th>")
                .append("<th class=\"col-desc\">D&#201;SIGNATION</th>")
                .append("<th class=\"col-qty\">QT&#201;</th>")
                .append("<th class=\"col-pu\">P.U. TTC</th>")
                .append("<th class=\"col-total\">MONTANT</th>")
                .append("</tr></thead><tbody>");

        if (refund != null && refund.getItems() != null) {
            int i = 1;
            for (RefundItem item : refund.getItems()) {
                String rowClass = (i % 2 == 0) ? "row-alt" : "";
                String name = item.getProduct() != null ? esc(item.getProduct().getName()) : "Article";

                html.append("<tr class=\"").append(rowClass).append("\">")
                        .append("<td class=\"center\">").append(i).append("</td>")
                        .append("<td>").append(name).append("</td>")
                        .append("<td class=\"center\">").append(item.getQuantity()).append("</td>")
                        .append("<td class=\"right\">").append(fmtCur(item.getUnitPrice())).append("</td>")
                        .append("<td class=\"right bold color-red\">-").append(fmtCur(item.getRefundAmount())).append("</td>")
                        .append("</tr>");
                i++;
            }
        } else if (order != null && order.getItems() != null) {
            // Fallback si pas de refund détaillé
            int i = 1;
            for (var item : order.getItems()) {
                String rowClass = (i % 2 == 0) ? "row-alt" : "";
                String name = item.getProduct() != null ? esc(item.getProduct().getName()) : "Article";

                html.append("<tr class=\"").append(rowClass).append("\">")
                        .append("<td class=\"center\">").append(i).append("</td>")
                        .append("<td>").append(name).append("</td>")
                        .append("<td class=\"center\">").append(item.getQuantity()).append("</td>")
                        .append("<td class=\"right\">").append(fmtCur(item.getUnitPrice())).append("</td>")
                        .append("<td class=\"right bold color-red\">-").append(fmtCur(item.getFinalPrice())).append("</td>")
                        .append("</tr>");
                i++;
            }
        }

        html.append("</tbody></table>");

        return this;
    }

    @Override
    public DocumentBuilder addTotals() {
        BigDecimal totalAmount = refund != null ? refund.getTotalRefundAmount() :
                (order != null ? order.getTotalAmount() : BigDecimal.ZERO);

        BigDecimal taux = order != null && order.getTaxRate() != null ? order.getTaxRate() : new BigDecimal("18.00");
        BigDecimal tva = totalAmount.multiply(taux).divide(new BigDecimal("118"), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal ht = totalAmount.subtract(tva);

        html.append("<table class=\"totals-grid\"><tr>")

                .append("<td class=\"totals-left\">")
                .append("<div class=\"sub-title\">D&#201;TAIL TVA</div>")
                .append("<table class=\"tva-table\">")
                .append("<thead><tr><th>BASE HT</th><th>TAUX</th><th>TVA</th></tr></thead>")
                .append("<tbody><tr>")
                .append("<td class=\"right\">-").append(fmtCur(ht)).append("</td>")
                .append("<td class=\"center\">").append(taux).append("%</td>")
                .append("<td class=\"right\">-").append(fmtCur(tva)).append("</td>")
                .append("</tr></tbody></table>")

                .append("<div class=\"accounting-info\">")
                .append("<b>Comptabilisation:</b><br/>")
                .append("Compte 411 (Clients): D&#233;bit<br/>")
                .append("Compte 707 (Ventes): Cr&#233;dit<br/>")
                .append("Compte 44571 (TVA collect&#233;e): Cr&#233;dit")
                .append("</div>")
                .append("</td>")

                .append("<td class=\"totals-right\">")
                .append("<table class=\"amounts-table\">");

        html.append(amtRow("Total HT", "-" + fmtCur(ht), ""));
        html.append(amtRow("TVA (" + taux + "%)", "-" + fmtCur(tva), ""));

        if (refund != null && refund.getRestockingFee() != null && refund.getRestockingFee().compareTo(BigDecimal.ZERO) > 0) {
            html.append(amtRow("Frais de restockage", fmtCur(refund.getRestockingFee()), ""));
        }

        html.append("<tr class=\"total-credit-row\">")
                .append("<td>TOTAL AVOIR TTC</td>")
                .append("<td class=\"right\">-").append(fmtCur(totalAmount)).append("</td>")
                .append("</tr>")
                .append("<tr><td colspan=\"2\" class=\"amount-words\">")
                .append("Montant &#224; d&#233;duire de votre prochain achat ou rembourser")
                .append("</td></tr>");

        // Mode de remboursement
        if (refund != null && refund.getRefundMethod() != null) {
            html.append("<tr class=\"refund-method-row\">")
                    .append("<td>Mode de remboursement</td>")
                    .append("<td class=\"right\">").append(esc(refund.getRefundMethod().toString())).append("</td>")
                    .append("</tr>");
        }

        html.append("</table></td></tr></table>");
        return this;
    }

    @Override
    public DocumentBuilder addFooter() {
        html.append("<div class=\"footer-divider\"></div>")
                .append("<table class=\"footer-grid\"><tr>")

                .append("<td class=\"footer-col\">")
                .append("<div class=\"sub-title\">CONDITIONS DE L'AVOIR</div>")
                .append("<div class=\"footer-text\">")
                .append("Cette note de cr&#233;dit est &#233;mise en contrepartie d'un remboursement. " +
                        "Elle peut &#234;tre utilis&#233;e pour un prochain achat ou faire l'objet d'un remboursement " +
                        "selon les modalit&#233;s convenues avec le client. D&#233;lai de traitement: 3 &#224; 5 jours ouvr&#233;s.")
                .append("</div>")
                .append("</td>")

                .append("<td class=\"footer-col signature-col\">")
                .append("<div class=\"sub-title\">POUR ").append(esc(companyName.toUpperCase())).append("</div>")
                .append("<div class=\"signature-box\">Signature et cachet</div>")
                .append("</td>")

                .append("<td class=\"footer-col footer-legal\">")
                .append("Document comptable g&#233;n&#233;r&#233; &#233;lectroniquement<br/>")
                .append("Conforme &#224; la r&#233;glementation OHADA<br/>")
                .append("&#169; ").append(java.time.Year.now()).append(" ").append(esc(companyName))
                .append("</td>")

                .append("</tr></table>")

                .append("<div class=\"footer-banner\">")
                .append("Ce document atteste d'un avoir &#224; votre favor &#8212; ").append(esc(companyWebsite))
                .append("</div>")
                .append("</body></html>");

        return this;
    }

    @Override
    public byte[] build() {
        try {
            String finalHtml = html.toString();
            ByteArrayOutputStream baos = (ByteArrayOutputStream) this.outputStream;
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(finalHtml, null);
            builder.toStream(baos);
            builder.run();
            log.info("Note de cr&#233;dit PDF g&#233;n&#233;r&#233;e ({} octets)", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur g&#233;n&#233;ration note de cr&#233;dit PDF", e);
            throw new RuntimeException("&#201;chec g&#233;n&#233;ration note de cr&#233;dit PDF", e);
        }
    }

    private String buildCss() {
        return
                "@page { size: A4; margin: 12mm 12mm 10mm 12mm; }" +
                        "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                        "body { font-family: Helvetica, Arial, sans-serif; font-size: 8pt; color: #1e2632; line-height: 1.35; }" +

                        // Header
                        ".credit-header { width: 100%; border-collapse: collapse; margin-bottom: 6pt; }" +
                        ".header-left { background: #742a2a; width: 60%; padding: 10pt 12pt; vertical-align: middle; }" +
                        ".header-right { background: #c53030; width: 40%; padding: 10pt 12pt; vertical-align: middle; }" +
                        ".company-name { font-size: 16pt; font-weight: bold; color: #ffffff; letter-spacing: 1px; margin-bottom: 2pt; }" +
                        ".company-sub { font-size: 7pt; color: #feb2b2; margin-bottom: 4pt; }" +
                        ".company-contacts { font-size: 7pt; color: #fc8181; line-height: 1.5; }" +
                        ".logo-img { max-height: 36pt; margin-bottom: 5pt; display: block; }" +
                        ".credit-badge { font-size: 24pt; font-weight: bold; color: #ffffff; text-align: right; letter-spacing: 2px; margin-bottom: 2pt; }" +
                        ".credit-subtitle { font-size: 9pt; color: #feb2b2; text-align: right; margin-bottom: 6pt; letter-spacing: 1px; }" +
                        ".meta-table { width: 100%; border-collapse: collapse; }" +
                        ".meta-table td { font-size: 7.5pt; padding: 1.5pt 0; color: #ffffff; }" +
                        ".meta-table td.lbl { color: #feb2b2; padding-right: 8pt; }" +
                        ".meta-table td.val { text-align: right; font-weight: bold; }" +

                        // Info grid
                        ".info-grid { width: 100%; border-collapse: collapse; margin: 7pt 0; }" +
                        ".info-box { width: 50%; padding: 6pt 8pt; vertical-align: top; border-left: 2.5pt solid #742a2a; background: #fff5f5; }" +
                        ".info-box--red { border-left-color: #c53030; background: #fed7d7; }" +
                        ".info-box__title { font-size: 7pt; font-weight: bold; color: #742a2a; margin-bottom: 3pt; }" +
                        ".info-box--red .info-box__title { color: #c53030; }" +
                        ".info-box__name { font-weight: bold; font-size: 8.5pt; margin-bottom: 2pt; }" +
                        ".info-box__body { font-size: 7.5pt; color: #3d4a58; line-height: 1.5; }" +
                        ".credit-status { background: #c53030; color: #ffffff; font-size: 7pt; font-weight: bold; padding: 3pt 6pt; margin-top: 4pt; text-align: center; }" +

                        // Section title
                        ".section-title { font-size: 8pt; font-weight: bold; color: #742a2a; margin: 6pt 0 3pt; border-bottom: 1pt solid #feb2b2; padding-bottom: 2pt; }" +

                        // Items table
                        ".items-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".items-table thead tr { background: #742a2a; color: #ffffff; }" +
                        ".items-table thead th { padding: 5pt 6pt; font-weight: bold; font-size: 7pt; }" +
                        ".col-num { width: 4%; text-align: center; }" +
                        ".col-desc { width: 46%; text-align: left; }" +
                        ".col-qty { width: 10%; text-align: center; }" +
                        ".col-pu { width: 20%; text-align: right; }" +
                        ".col-total { width: 20%; text-align: right; }" +
                        ".items-table tbody td { padding: 4pt 6pt; vertical-align: top; border-bottom: 0.5pt solid #fed7d7; }" +
                        ".row-alt { background: #fff5f5; }" +

                        // Totals grid
                        ".totals-grid { width: 100%; border-collapse: collapse; margin-top: 4pt; }" +
                        ".totals-left { width: 55%; vertical-align: top; padding-right: 10pt; }" +
                        ".totals-right { width: 45%; vertical-align: top; }" +
                        ".sub-title { font-size: 7pt; font-weight: bold; color: #742a2a; margin-bottom: 3pt; }" +
                        ".tva-table { width: 90%; border-collapse: collapse; font-size: 7pt; margin-bottom: 6pt; }" +
                        ".tva-table thead th { background: #c53030; color: #fff; padding: 3pt 5pt; font-weight: bold; }" +
                        ".tva-table tbody td { padding: 3pt 5pt; border-bottom: 0.5pt solid #fed7d7; background: #fff5f5; }" +
                        ".accounting-info { font-size: 7pt; color: #4a5568; line-height: 1.6; background: #f7fafc; padding: 4pt 6pt; border-left: 2pt solid #718096; }" +
                        ".amounts-table { width: 100%; border-collapse: collapse; font-size: 8pt; }" +
                        ".amounts-table td { padding: 3pt 4pt; color: #5a6475; }" +
                        ".amounts-table td + td { text-align: right; color: #1e2632; }" +
                        ".total-credit-row td { font-size: 12pt; font-weight: bold; color: #c53030; padding: 5pt 4pt; border-top: 1.5pt solid #c53030; border-bottom: 1.5pt solid #c53030; }" +
                        ".amount-words { font-size: 6.5pt; color: #718096; font-style: italic; padding: 3pt 0 5pt; }" +
                        ".refund-method-row td { font-size: 8pt; font-weight: bold; color: #2f855a; background: #f0fff4; padding: 4pt; }" +
                        ".color-red { color: #c53030; font-weight: bold; }" +

                        // Footer
                        ".footer-divider { border-top: 0.5pt solid #feb2b2; margin: 6pt 0; }" +
                        ".footer-grid { width: 100%; border-collapse: collapse; }" +
                        ".footer-col { width: 40%; vertical-align: top; padding-right: 8pt; font-size: 7pt; }" +
                        ".footer-text { color: #4a5568; line-height: 1.5; }" +
                        ".signature-col { width: 25%; text-align: center; }" +
                        ".signature-box { border: 0.5pt solid #feb2b2; height: 32pt; color: #a0aec0; font-style: italic; font-size: 6.5pt; text-align: center; padding-top: 24pt; }" +
                        ".footer-legal { width: 35%; color: #718096; line-height: 1.7; text-align: right; font-size: 6.5pt; }" +
                        ".footer-banner { background: #742a2a; color: #ffffff; text-align: center; font-size: 7.5pt; font-style: italic; padding: 5pt; margin-top: 7pt; }" +

                        // Utilities
                        ".center { text-align: center; }" +
                        ".right { text-align: right; }" +
                        ".bold { font-weight: bold; }";
    }

    private String metaRow(String label, String value) {
        return "<tr><td class=\"lbl\">" + label + "</td><td class=\"val\">" + value + "</td></tr>";
    }

    private String amtRow(String label, String value, String css) {
        String valCss = "right" + (css.isEmpty() ? "" : " " + css);
        return "<tr><td" + (css.isEmpty() ? "" : " class=\"" + css + "\"") + ">" + label + "</td>"
                + "<td class=\"" + valCss + "\">" + value + "</td></tr>";
    }

    private String buildLogoTag() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(logoPath);
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                String b64  = Base64.getEncoder().encodeToString(bytes);
                String mime = logoPath.endsWith(".png") ? "image/png" : "image/jpeg";
                return "<img class=\"logo-img\" src=\"data:" + mime + ";base64," + b64 + "\"/>";
            }
        } catch (IOException e) {
            log.debug("Logo avoir non trouv&#233;: {}", logoPath);
        }
        return "";
    }

    protected String fmtCur(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.0f FCFA", amount);
    }

    protected String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
