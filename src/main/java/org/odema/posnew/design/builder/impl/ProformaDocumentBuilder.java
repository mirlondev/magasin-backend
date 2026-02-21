package org.odema.posnew.design.builder.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.domain.model.Customer;
import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.OrderItem;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Builder pour les proformas (devis).
 * Format A4 professionnel - Document commercial sans valeur fiscale.
 */
@Slf4j
@Component
public class ProformaDocumentBuilder extends AbstractPdfDocumentBuilder {

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DT   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String companyName        = "ODEMA POS";
    private String companyAddress     = "123 Rue Principale, Pointe-Noire";
    private String companyPhone       = "+237 6XX XX XX XX";
    private String companyEmail       = "contact@odema.com";
    private String companyTaxId       = "TAX-123456789";
    private String companyBankAccount = "CM21 1234 5678 9012 3456 7890 123";
    private String companyBankName    = "Banque Atlantique";
    private String companyWebsite     = "www.odema.com";
    private String logoPath           = "static/logo.png";

    private String proformaNumber;
    private LocalDateTime validityDate;
    private String notes;
    private final StringBuilder html = new StringBuilder();

    public ProformaDocumentBuilder() { super(null); }

    public ProformaDocumentBuilder(Order order) {
        super(order);
    }

    public ProformaDocumentBuilder withConfig(
            String companyName, String companyAddress,
            String companyPhone, String companyEmail,
            String companyTaxId, String companyBankAccount,
            String companyBankName, String companyWebsite) {
        if (companyName        != null) this.companyName        = companyName;
        if (companyAddress     != null) this.companyAddress     = companyAddress;
        if (companyPhone       != null) this.companyPhone       = companyPhone;
        if (companyEmail       != null) this.companyEmail       = companyEmail;
        if (companyTaxId       != null) this.companyTaxId       = companyTaxId;
        if (companyBankAccount != null) this.companyBankAccount = companyBankAccount;
        if (companyBankName    != null) this.companyBankName    = companyBankName;
        if (companyWebsite     != null) this.companyWebsite     = companyWebsite;
        return this;
    }

    public ProformaDocumentBuilder withProformaNumber(String number) {
        this.proformaNumber = number;
        return this;
    }

    public ProformaDocumentBuilder withValidityDays(int days) {
        this.validityDate = LocalDateTime.now().plusDays(days);
        return this;
    }

    public ProformaDocumentBuilder withNotes(String notes) {
        this.notes = notes;
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
        log.debug("ProformaDocumentBuilder HTML initialis&#233;");
        return this;
    }

    @Override
    public DocumentBuilder addHeader() {
        String logoHtml = buildLogoTag();

        html.append("<table class=\"proforma-header\"><tr>")
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
                .append("<div class=\"proforma-badge\">PROFORMA</div>")
                .append("<div class=\"proforma-watermark\">DEVIS</div>")
                .append("<table class=\"meta-table\">")
                .append(metaRow("N&#176; Proforma", esc(proformaNumber != null ? proformaNumber : "PRO-" + (order != null ? order.getOrderNumber() : "TEMP"))))
                .append(metaRow("&#201;mission", LocalDateTime.now().format(FMT_DATE)))
                .append(metaRow("Valable jusqu'au", validityDate != null ? validityDate.format(FMT_DATE) : "&#8212;"))
                .append(metaRow("R&#233;f&#233;rence", order != null ? esc(order.getOrderNumber()) : "&#8212;"))
                .append("</table>")
                .append("</td>")
                .append("</tr></table>");

        return this;
    }

    @Override
    public DocumentBuilder addMainInfo() {
        Customer c = order != null ? order.getCustomer() : null;

        html.append("<table class=\"info-grid\"><tr>")
                .append("<td class=\"info-box info-box--navy\">")
                .append("<div class=\"info-box__title\">FACTURER &#192;</div>")
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
                .append("<td class=\"info-box info-box--orange\">")
                .append("<div class=\"info-box__title\">CONDITIONS</div>")
                .append("<div class=\"info-box__body\">")
                .append("<b>Validit&#233;:</b> 30 jours<br/>")
                .append("<b>Paiement:</b> 50% &#224; commande, 50% &#224; livraison<br/>")
                .append("<b>Livraison:</b> 3-5 jours ouvr&#233;s<br/>")
                .append("<b>Garantie:</b> 12 mois")
                .append("</div>")
                .append("</td>")
                .append("</tr></table>");

        // Notes personnalisées
        if (notes != null && !notes.isBlank()) {
            html.append("<div class=\"notes-box\">")
                    .append("<div class=\"notes-title\">&#128712; NOTES SP&#201;CIALES</div>")
                    .append("<div class=\"notes-body\">").append(esc(notes)).append("</div>")
                    .append("</div>");
        }

        return this;
    }

    @Override
    public DocumentBuilder addItemsTable() {
        html.append("<div class=\"section-title\">D&#201;TAIL DES ARTICLES</div>")
                .append("<table class=\"items-table\">")
                .append("<thead><tr>")
                .append("<th class=\"col-num\">N&#176;</th>")
                .append("<th class=\"col-desc\">D&#201;SIGNATION</th>")
                .append("<th class=\"col-qty\">QT&#201;</th>")
                .append("<th class=\"col-pu\">P.U. HT</th>")
                .append("<th class=\"col-total\">TOTAL HT</th>")
                .append("</tr></thead><tbody>");

        if (order != null && order.getItems() != null) {
            int i = 1;
            for (OrderItem item : order.getItems()) {
                String rowClass = (i % 2 == 0) ? "row-alt" : "";
                String name = item.getProduct() != null ? esc(item.getProduct().getName()) : "Article";

                // SKU
                String sku = "";
                if (item.getProduct() != null && item.getProduct().getSku() != null) {
                    sku = "<span class=\"sku\">R&#233;f: " + esc(item.getProduct().getSku()) + "</span>";
                }

                html.append("<tr class=\"").append(rowClass).append("\">")
                        .append("<td class=\"center\">").append(i).append("</td>")
                        .append("<td>").append(name).append(sku).append("</td>")
                        .append("<td class=\"center\">").append(item.getQuantity()).append("</td>")
                        .append("<td class=\"right\">").append(fmtCur(calcHT(item.getUnitPrice()))).append("</td>")
                        .append("<td class=\"right bold\">").append(fmtCur(calcHT(item.getFinalPrice()))).append("</td>")
                        .append("</tr>");
                i++;
            }
        }

        int totalQty = order != null ? order.getItems().stream().mapToInt(OrderItem::getQuantity).sum() : 0;
        html.append("</tbody></table>")
                .append("<div class=\"items-summary\">")
                .append(order != null ? order.getItems().size() : 0).append(" article(s) &#160;&#183;&#160; ")
                .append(totalQty).append(" unit&#233;(s)")
                .append("</div>");

        return this;
    }

    @Override
    public DocumentBuilder addTotals() {
        BigDecimal taux = order != null && order.getTaxAmount() != null ? order.getTaxAmount() : new BigDecimal("18.00");
        BigDecimal subtotalHT = order != null ? calcHT(order.getTotalAmount()) : BigDecimal.ZERO;
        BigDecimal tva = subtotalHT.multiply(taux).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalTTC = subtotalHT.add(tva);

        html.append("<table class=\"totals-grid\"><tr>")

                .append("<td class=\"totals-left\">")
                .append("<div class=\"sub-title\">D&#201;TAIL TVA</div>")
                .append("<table class=\"tva-table\">")
                .append("<thead><tr><th>BASE HT</th><th>TAUX</th><th>TVA</th></tr></thead>")
                .append("<tbody><tr>")
                .append("<td class=\"right\">").append(fmtCur(subtotalHT)).append("</td>")
                .append("<td class=\"center\">").append(taux).append("%</td>")
                .append("<td class=\"right\">").append(fmtCur(tva)).append("</td>")
                .append("</tr></tbody></table>")

                .append("<div class=\"sub-title\" style=\"margin-top:8px\">COORDONN&#201;ES BANCAIRES</div>")
                .append("<div class=\"bank-info\">")
                .append("<b>").append(esc(companyBankName)).append("</b><br/>")
                .append("IBAN: ").append(esc(companyBankAccount)).append("<br/>")
                .append("B&#233;n&#233;ficiaire: <b>").append(esc(companyName)).append("</b>")
                .append("</div>")
                .append("</td>")

                .append("<td class=\"totals-right\">")
                .append("<table class=\"amounts-table\">");

        html.append(amtRow("Sous-total HT", fmtCur(subtotalHT), ""));

        if (order != null && order.getGlobalDiscountAmount() != null && order.getGlobalDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            html.append(amtRow("Remise", "-" + fmtCur(order.getGlobalDiscountAmount()), "color-red"));
        }

        html.append(amtRow("TVA (" + taux + "%)", fmtCur(tva), ""));

        html.append("<tr class=\"total-ttc-row\">")
                .append("<td>TOTAL ESTIM&#201; TTC</td>")
                .append("<td class=\"right\">").append(fmtCur(totalTTC)).append("</td>")
                .append("</tr>")
                .append("<tr><td colspan=\"2\" class=\"amount-words\">")
                .append("Ce montant est donn&#233; &#224; titre indicatif")
                .append("</td></tr>");

        // Acompte suggéré
        BigDecimal deposit = totalTTC.multiply(new BigDecimal("0.50")).setScale(0, RoundingMode.HALF_UP);
        html.append("<tr class=\"deposit-row\">")
                .append("<td>Acompte sugg&#233;r&#233; (50%)</td>")
                .append("<td class=\"right\">").append(fmtCur(deposit)).append("</td>")
                .append("</tr>");

        html.append("</table></td></tr></table>");
        return this;
    }

    @Override
    public DocumentBuilder addFooter() {
        html.append("<div class=\"footer-divider\"></div>")
                .append("<table class=\"footer-grid\"><tr>")

                .append("<td class=\"footer-col\">")
                .append("<div class=\"sub-title\">ACCEPTATION DU DEVIS</div>")
                .append("<div class=\"acceptance-box\">")
                .append("Bon pour accord le ________________<br/><br/>")
                .append("Signature: ________________________<br/><br/>")
                .append("Nom et qualit&#233;: ____________________")
                .append("</div>")
                .append("</td>")

                .append("<td class=\"footer-col footer-legal\">")
                .append("<div class=\"warning-proforma\">&#9888; DOCUMENT SANS VALEUR FISCALE</div>")
                .append("Ce proforma est un devis estimatif et ne constitue pas une facture. " +
                        "Les prix et disponibilit&#233;s sont sujets &#224; confirmation. " +
                        "Une commande ferme requiert un bon de commande sign&#233;.")
                .append("</td>")

                .append("</tr></table>")

                .append("<div class=\"footer-banner\">")
                .append("Merci pour votre int&#233;r&#234;t &#8212; ").append(esc(companyWebsite))
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
            log.info("Proforma PDF g&#233;n&#233;r&#233; ({} octets)", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur g&#233;n&#233;ration proforma PDF", e);
            throw new RuntimeException("&#201;chec g&#233;n&#233;ration proforma PDF", e);
        }
    }

    private String buildCss() {
        return
                "@page { size: A4; margin: 12mm 12mm 10mm 12mm; }" +
                        "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                        "body { font-family: Helvetica, Arial, sans-serif; font-size: 8pt; color: #1e2632; line-height: 1.35; }" +

                        // Header avec watermark
                        ".proforma-header { width: 100%; border-collapse: collapse; margin-bottom: 6pt; }" +
                        ".header-left { background: #744210; width: 60%; padding: 10pt 12pt; vertical-align: middle; }" +
                        ".header-right { background: #d69e2e; width: 40%; padding: 10pt 12pt; vertical-align: middle; position: relative; overflow: hidden; }" +
                        ".company-name { font-size: 16pt; font-weight: bold; color: #ffffff; letter-spacing: 1px; margin-bottom: 2pt; }" +
                        ".company-sub { font-size: 7pt; color: #fbd38d; margin-bottom: 4pt; }" +
                        ".company-contacts { font-size: 7pt; color: #f6ad55; line-height: 1.5; }" +
                        ".logo-img { max-height: 36pt; margin-bottom: 5pt; display: block; }" +
                        ".proforma-badge { font-size: 20pt; font-weight: bold; color: #ffffff; text-align: right; letter-spacing: 2px; margin-bottom: 6pt; position: relative; z-index: 2; }" +
                        ".proforma-watermark { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-15deg); font-size: 48pt; color: rgba(255,255,255,0.15); font-weight: bold; z-index: 1; }" +
                        ".meta-table { width: 100%; border-collapse: collapse; position: relative; z-index: 2; }" +
                        ".meta-table td { font-size: 7.5pt; padding: 1.5pt 0; color: #ffffff; }" +
                        ".meta-table td.lbl { color: #fbd38d; padding-right: 8pt; }" +
                        ".meta-table td.val { text-align: right; font-weight: bold; }" +

                        // Info grid
                        ".info-grid { width: 100%; border-collapse: collapse; margin: 7pt 0; }" +
                        ".info-box { width: 50%; padding: 6pt 8pt; vertical-align: top; border-left: 2.5pt solid #744210; background: #fffaf0; }" +
                        ".info-box--orange { border-left-color: #d69e2e; background: #fffbeb; }" +
                        ".info-box__title { font-size: 7pt; font-weight: bold; color: #744210; margin-bottom: 3pt; }" +
                        ".info-box--orange .info-box__title { color: #d69e2e; }" +
                        ".info-box__name { font-weight: bold; font-size: 8.5pt; margin-bottom: 2pt; }" +
                        ".info-box__body { font-size: 7.5pt; color: #3d4a58; line-height: 1.5; }" +

                        // Notes
                        ".notes-box { background: #ebf8ff; border-left: 2.5pt solid #4299e1; padding: 5pt 8pt; margin: 6pt 0; }" +
                        ".notes-title { font-size: 7pt; font-weight: bold; color: #2b6cb0; margin-bottom: 2pt; }" +
                        ".notes-body { font-size: 7.5pt; color: #2c5282; line-height: 1.4; }" +

                        // Section title
                        ".section-title { font-size: 8pt; font-weight: bold; color: #744210; margin: 6pt 0 3pt; border-bottom: 1pt solid #fbd38d; padding-bottom: 2pt; }" +

                        // Items table
                        ".items-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".items-table thead tr { background: #744210; color: #ffffff; }" +
                        ".items-table thead th { padding: 5pt 6pt; font-weight: bold; font-size: 7pt; }" +
                        ".col-num { width: 4%; text-align: center; }" +
                        ".col-desc { width: 46%; text-align: left; }" +
                        ".col-qty { width: 10%; text-align: center; }" +
                        ".col-pu { width: 20%; text-align: right; }" +
                        ".col-total { width: 20%; text-align: right; }" +
                        ".items-table tbody td { padding: 4pt 6pt; vertical-align: top; border-bottom: 0.5pt solid #fef3c7; }" +
                        ".row-alt { background: #fffbeb; }" +
                        ".sku { display: block; font-size: 6.5pt; color: #a0aec0; margin-top: 1pt; }" +
                        ".items-summary { text-align: right; font-size: 7pt; color: #718096; font-style: italic; margin: 3pt 0 6pt; }" +

                        // Totals grid
                        ".totals-grid { width: 100%; border-collapse: collapse; margin-top: 4pt; }" +
                        ".totals-left { width: 58%; vertical-align: top; padding-right: 10pt; }" +
                        ".totals-right { width: 42%; vertical-align: top; }" +
                        ".sub-title { font-size: 7pt; font-weight: bold; color: #744210; margin-bottom: 3pt; }" +
                        ".tva-table { width: 85%; border-collapse: collapse; font-size: 7pt; margin-bottom: 6pt; }" +
                        ".tva-table thead th { background: #d69e2e; color: #fff; padding: 3pt 5pt; font-weight: bold; }" +
                        ".tva-table tbody td { padding: 3pt 5pt; border-bottom: 0.5pt solid #fef3c7; background: #fffbeb; }" +
                        ".bank-info { font-size: 7.5pt; color: #4a5568; line-height: 1.6; background: #fffbeb; padding: 4pt 6pt; border-left: 2pt solid #d69e2e; }" +
                        ".amounts-table { width: 100%; border-collapse: collapse; font-size: 8pt; }" +
                        ".amounts-table td { padding: 3pt 4pt; color: #5a6475; }" +
                        ".amounts-table td + td { text-align: right; color: #1e2632; }" +
                        ".total-ttc-row td { font-size: 12pt; font-weight: bold; color: #744210; padding: 5pt 4pt; border-top: 1.5pt solid #744210; border-bottom: 1.5pt solid #744210; }" +
                        ".amount-words { font-size: 6.5pt; color: #a0aec0; font-style: italic; padding: 3pt 0 5pt; }" +
                        ".deposit-row td { font-size: 9pt; font-weight: bold; color: #2f855a; background: #f0fff4; padding: 4pt; margin-top: 4pt; }" +
                        ".color-red { color: #c53030; font-weight: bold; }" +

                        // Footer
                        ".footer-divider { border-top: 0.5pt solid #fbd38d; margin: 6pt 0; }" +
                        ".footer-grid { width: 100%; border-collapse: collapse; }" +
                        ".footer-col { width: 50%; vertical-align: top; padding-right: 8pt; font-size: 7pt; }" +
                        ".acceptance-box { border: 0.5pt solid #d69e2e; padding: 8pt; font-size: 7.5pt; color: #4a5568; line-height: 1.8; }" +
                        ".footer-legal { color: #744210; line-height: 1.6; font-size: 7pt; }" +
                        ".warning-proforma { background: #c53030; color: #ffffff; font-size: 7pt; font-weight: bold; padding: 3pt 6pt; margin-bottom: 4pt; text-align: center; }" +
                        ".footer-banner { background: #744210; color: #ffffff; text-align: center; font-size: 7.5pt; font-style: italic; padding: 5pt; margin-top: 7pt; }" +

                        // Utilities
                        ".center { text-align: center; }" +
                        ".right { text-align: right; }" +
                        ".bold { font-weight: bold; }" +
                        ".muted { color: #a0aec0; }";
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
            log.debug("Logo proforma non trouv&#233;: {}", logoPath);
        }
        return "";
    }

    protected String fmtCur(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.0f FCFA", amount);
    }

    private BigDecimal calcHT(BigDecimal ttc) {
        if (ttc == null) return BigDecimal.ZERO;
        return ttc.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
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
