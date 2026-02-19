package org.odema.posnew.design.builder.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.entity.Customer;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.OrderItem;
import org.odema.posnew.entity.Payment;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InvoiceDocumentBuilder extends AbstractPdfDocumentBuilder {

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
    private String companyLogoPath    = "static/logo.png";

    private final StringBuilder html = new StringBuilder();

    public InvoiceDocumentBuilder() { super(null); }
    public InvoiceDocumentBuilder(Order order) { super(order); }

    public InvoiceDocumentBuilder withConfig(
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

    // ════════════════════════════════════════════════════════════════════════════
    // INITIALIZE
    //
    // FIX #1 — openhtmltopdf attend du XHTML valide (parser SAX strict).
    //   → Toujours commencer par <?xml ...?> + DOCTYPE XHTML + xmlns
    //   → Ne JAMAIS utiliser &nbsp; &mdash; &middot; directement dans le HTML :
    //     ce sont des entités HTML4 inconnues du parser XML.
    //     Utiliser leurs équivalents numériques : &#160; &#8212; &#183; etc.
    // ════════════════════════════════════════════════════════════════════════════
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
        log.debug("InvoiceDocumentBuilder HTML initialisé");
        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HEADER
    // Note: utiliser des tables HTML (pas display:table) — meilleur support XHTML
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public DocumentBuilder addHeader() {
        String logoHtml = buildLogoTag();

        html.append("<table class=\"invoice-header\"><tr>")
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
                .append("<div class=\"invoice-badge\">FACTURE</div>")
                .append("<table class=\"meta-table\">")
                .append(metaRow("N&#176;",         esc(order.getOrderNumber())))
                .append(metaRow("&#201;mission",   LocalDateTime.now().format(FMT_DATE)))
                .append(metaRow("&#201;ch&#233;ance", LocalDateTime.now().plusDays(30).format(FMT_DATE)))
                .append(metaRow("R&#233;f&#233;rence", "REF-" + safeRef()))
                .append("</table>")
                .append("</td>")
                .append("</tr></table>");

        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MAIN INFO
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public DocumentBuilder addMainInfo() {
        Customer c = order.getCustomer();

        // FIX #2 — && en Java est correct dans le code Java.
        // En revanche dans les *strings HTML* : ne jamais écrire & seul → &amp;
        // La méthode esc() s'en charge pour les données dynamiques.
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
        html.append("T&#233;l: ").append(c != null ? nvl(c.getPhone()) : "&#8212;").append("<br/>");
        if (c != null && c.getEmail() != null) {
            html.append(esc(c.getEmail()));
        }

        html.append("</div>")
                .append("</td>")
                .append("<td class=\"info-box info-box--blue\">")
                .append("<div class=\"info-box__title\">POINT DE VENTE</div>")
                .append("<div class=\"info-box__body\">")
                .append("<b>Magasin:</b> ")
                .append(order.getStore() != null ? esc(order.getStore().getName()) : "&#8212;")
                .append("<br/><b>Vendeur:</b> ")
                .append(order.getCashier() != null ? esc(order.getCashier().getFullName()) : "&#8212;")
                .append("<br/><b>Date:</b> ").append(order.getCreatedAt().format(FMT_DT))
                .append("<br/><b>Livraison:</b> Retrait en magasin")
                .append("</div>")
                .append("</td>")
                .append("</tr></table>");

        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ITEMS TABLE
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public DocumentBuilder addItemsTable() {
        html.append("<div class=\"section-title\">D&#201;TAIL DES ARTICLES</div>")
                .append("<table class=\"items-table\">")
                .append("<thead><tr>")
                .append("<th class=\"col-num\">N&#176;</th>")
                .append("<th class=\"col-desc\">D&#201;SIGNATION</th>")
                .append("<th class=\"col-qty\">QT&#201;</th>")
                .append("<th class=\"col-pu\">P.U. HT</th>")
                .append("<th class=\"col-disc\">REMISE</th>")
                .append("<th class=\"col-total\">TOTAL TTC</th>")
                .append("</tr></thead><tbody>");

        int i = 1;
        for (OrderItem item : order.getItems()) {
            String rowClass = (i % 2 == 0) ? "row-alt" : "";
            String name     = item.getProduct() != null ? esc(item.getProduct().getName()) : "Article";

            // SKU et notes dans des blocs séparés — esc() obligatoire
            String sku = "";
            if (item.getProduct() != null && item.getProduct().getSku() != null) {
                sku = "<span class=\"sku\">R&#233;f: " + esc(item.getProduct().getSku()) + "</span>";
            }
            String note = "";
            if (item.getNotes() != null && !item.getNotes().isEmpty()) {
                note = "<span class=\"note\">&#8594; " + esc(item.getNotes()) + "</span>";
            }

            html.append("<tr class=\"").append(rowClass).append("\">")
                    .append("<td class=\"center\">").append(i).append("</td>")
                    .append("<td>").append(name).append(sku).append(note).append("</td>")
                    .append("<td class=\"center\">").append(item.getQuantity()).append("</td>")
                    .append("<td class=\"right\">").append(fmtCur(calcHT(item.getUnitPrice()))).append("</td>")
                    .append("<td class=\"center muted\">&#8212;</td>")
                    .append("<td class=\"right bold\">").append(fmtCur(item.getFinalPrice())).append("</td>")
                    .append("</tr>");
            i++;
        }

        int totalQty = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        html.append("</tbody></table>")
                .append("<div class=\"items-summary\">")
                .append(order.getItems().size()).append(" article(s) &#160;&#183;&#160; ")
                .append(totalQty).append(" unit&#233;(s)")
                .append("</div>");

        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TOTALS
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public DocumentBuilder addTotals() {
        BigDecimal taux = order.getTaxRate() != null ? order.getTaxRate() : new BigDecimal("18.00");

        List<Payment> paidList = order.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .collect(Collectors.toList());
        BigDecimal totalPaid = paidList.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = order.getTotalAmount().subtract(totalPaid);

        html.append("<table class=\"totals-grid\"><tr>")

                .append("<td class=\"totals-left\">")
                .append("<div class=\"sub-title\">D&#201;TAIL TVA</div>")
                .append("<table class=\"tva-table\">")
                .append("<thead><tr><th>BASE HT</th><th>TAUX</th><th>TVA</th></tr></thead>")
                .append("<tbody><tr>")
                .append("<td class=\"right\">").append(fmtCur(order.getSubtotal())).append("</td>")
                .append("<td class=\"center\">").append(taux).append("%</td>")
                .append("<td class=\"right\">").append(fmtCur(order.getTaxAmount())).append("</td>")
                .append("</tr></tbody></table>")

                .append("<div class=\"sub-title\" style=\"margin-top:8px\">R&#200;GLEMENT PAR VIREMENT</div>")
                .append("<div class=\"bank-info\">")
                .append("<b>").append(esc(companyBankName)).append("</b><br/>")
                .append("IBAN: ").append(esc(companyBankAccount)).append("<br/>")
                .append("B&#233;n&#233;ficiaire: <b>").append(esc(companyName)).append("</b>")
                .append("</div>")

                .append("<div class=\"payment-methods\">")
                .append("Carte &#183; Mobile Money &#183; Virement &#183; Esp&#232;ces")
                .append("</div>")
                .append("</td>")

                .append("<td class=\"totals-right\">")
                .append("<table class=\"amounts-table\">");

        html.append(amtRow("Sous-total HT", fmtCur(order.getSubtotal()), ""));

        if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            html.append(amtRow("Remise", "-" + fmtCur(order.getDiscountAmount()), "color-red"));
        }
        if (order.getTaxAmount() != null && order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
            html.append(amtRow("TVA (" + taux + "%)", fmtCur(order.getTaxAmount()), ""));
        }

        html.append("<tr class=\"total-ttc-row\">")
                .append("<td>TOTAL TTC</td>")
                .append("<td class=\"right\">").append(fmtCur(order.getTotalAmount())).append("</td>")
                .append("</tr>")
                .append("<tr><td colspan=\"2\" class=\"amount-words\">")
                .append("Arr&#234;t&#233; &#224; : ").append(fmtCur(order.getTotalAmount()))
                .append("</td></tr>");

        if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            html.append(amtRow("D&#233;j&#224; pay&#233;", fmtCur(totalPaid), "color-green"));
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                html.append(amtRow("Reste &#224; payer", fmtCur(remaining), "color-red bold-val"));
            } else if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                html.append(amtRow("Monnaie rendue", fmtCur(remaining.abs()), "color-green"));
            } else {
                html.append("<tr><td colspan=\"2\">")
                        .append("<div class=\"status-badge status-paid\">&#10003; PAY&#201; EN TOTALIT&#201;</div>")
                        .append("</td></tr>");
            }
        } else {
            html.append("<tr><td colspan=\"2\">")
                    .append("<div class=\"status-badge status-unpaid\">&#9888; NON PAY&#201;</div>")
                    .append("</td></tr>");
        }

        html.append("</table></td></tr></table>");
        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // FOOTER
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public DocumentBuilder addFooter() {
        html.append("<div class=\"footer-divider\"></div>")
                .append("<table class=\"footer-grid\"><tr>")

                .append("<td class=\"footer-col\">")
                .append("<div class=\"sub-title\">CONDITIONS G&#201;N&#201;RALES DE VENTE</div>")
                .append("<div class=\"footer-text\">")
                .append("Paiement comptant &#224; r&#233;ception.<br/>")
                .append("Escompte 2% pour paiement anticip&#233; sous 8 jours.<br/>")
                .append("P&#233;nalit&#233;s de retard: 3x le taux l&#233;gal.<br/>")
                .append("R&#233;serve de propri&#233;t&#233; jusqu'au paiement int&#233;gral.<br/>")
                .append("Tribunal comp&#233;tent: Commerce de Pointe-Noire.")
                .append("</div>")
                .append("</td>")

                .append("<td class=\"footer-col signature-col\">")
                .append("<div class=\"sub-title\">POUR ").append(esc(companyName.toUpperCase())).append("</div>")
                .append("<div class=\"signature-box\">Signature et cachet</div>")
                .append("</td>")

                .append("<td class=\"footer-col footer-legal\">")
                .append("Document g&#233;n&#233;r&#233; &#233;lectroniquement<br/>")
                .append("Valide sans signature manuscrite<br/>")
                .append("RCCM: Pointe-Noire<br/>")
                .append("&#169; ").append(java.time.Year.now()).append(" ").append(esc(companyName))
                .append("</td>")

                .append("</tr></table>")
                .append("<div class=\"footer-banner\">")
                .append("Merci pour votre confiance &#8212; ").append(esc(companyWebsite))
                .append("</div>")
                .append("</body></html>");

        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // BUILD
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public byte[] build() {
        try {
            String finalHtml = html.toString();
            ByteArrayOutputStream baos = (ByteArrayOutputStream) this.outputStream;
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(finalHtml, null);
            builder.toStream(baos);
            builder.run();
            log.info("Facture PDF générée ({} octets)", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur génération PDF: {}", e.getMessage());
            throw new RuntimeException("Échec génération facture PDF", e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CSS — concaténation de strings Java (pas de text block pour éviter
    //        tout risque de caractère spécial non échappé dans le CSS)
    // ════════════════════════════════════════════════════════════════════════════
    private String buildCss() {
        return
                "@page { size: A4; margin: 12mm 12mm 10mm 12mm; }" +
                        "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                        "body { font-family: Helvetica, Arial, sans-serif; font-size: 8pt; color: #1e2632; line-height: 1.35; }" +
                        ".invoice-header { width: 100%; border-collapse: collapse; margin-bottom: 6pt; }" +
                        ".header-left { background: #15294d; width: 60%; padding: 10pt 12pt; vertical-align: middle; }" +
                        ".header-right { background: #1e5aa0; width: 40%; padding: 10pt 12pt; vertical-align: middle; }" +
                        ".company-name { font-size: 16pt; font-weight: bold; color: #ffffff; letter-spacing: 1px; margin-bottom: 2pt; }" +
                        ".company-sub { font-size: 7pt; color: #b4c8e1; margin-bottom: 4pt; }" +
                        ".company-contacts { font-size: 7pt; color: #8aadd4; line-height: 1.5; }" +
                        ".logo-img { max-height: 36pt; margin-bottom: 5pt; display: block; }" +
                        ".invoice-badge { font-size: 22pt; font-weight: bold; color: #ffffff; text-align: right; letter-spacing: 2px; margin-bottom: 6pt; }" +
                        ".meta-table { width: 100%; border-collapse: collapse; }" +
                        ".meta-table td { font-size: 7.5pt; padding: 1.5pt 0; color: #ffffff; }" +
                        ".meta-table td.lbl { color: #b4c8e1; padding-right: 8pt; }" +
                        ".meta-table td.val { text-align: right; font-weight: bold; }" +
                        ".info-grid { width: 100%; border-collapse: collapse; margin: 7pt 0; }" +
                        ".info-box { width: 50%; padding: 6pt 8pt; vertical-align: top; border-left: 2.5pt solid #15294d; background: #f8fafd; }" +
                        ".info-box--blue { border-left-color: #1e5aa0; }" +
                        ".info-box__title { font-size: 7pt; font-weight: bold; color: #15294d; margin-bottom: 3pt; }" +
                        ".info-box--blue .info-box__title { color: #1e5aa0; }" +
                        ".info-box__name { font-weight: bold; font-size: 8.5pt; margin-bottom: 2pt; }" +
                        ".info-box__body { font-size: 7.5pt; color: #3d4a58; line-height: 1.5; }" +
                        ".section-title { font-size: 8pt; font-weight: bold; color: #15294d; margin: 6pt 0 3pt; border-bottom: 1pt solid #c8d4e6; padding-bottom: 2pt; }" +
                        ".items-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".items-table thead tr { background: #15294d; color: #ffffff; }" +
                        ".items-table thead th { padding: 5pt 6pt; font-weight: bold; font-size: 7pt; }" +
                        ".col-num { width: 4%; text-align: center; }" +
                        ".col-desc { width: 38%; text-align: left; }" +
                        ".col-qty { width: 7%; text-align: center; }" +
                        ".col-pu { width: 14%; text-align: right; }" +
                        ".col-disc { width: 10%; text-align: center; }" +
                        ".col-total { width: 14%; text-align: right; }" +
                        ".items-table tbody td { padding: 4pt 6pt; vertical-align: top; border-bottom: 0.5pt solid #e8eef6; }" +
                        ".row-alt { background: #f0f4fa; }" +
                        ".sku { display: block; font-size: 6.5pt; color: #7a8898; margin-top: 1pt; }" +
                        ".note { display: block; font-size: 6.5pt; color: #1e5aa0; font-style: italic; }" +
                        ".items-summary { text-align: right; font-size: 7pt; color: #7a8898; font-style: italic; margin: 3pt 0 6pt; }" +
                        ".totals-grid { width: 100%; border-collapse: collapse; margin-top: 4pt; }" +
                        ".totals-left { width: 58%; vertical-align: top; padding-right: 10pt; }" +
                        ".totals-right { width: 42%; vertical-align: top; }" +
                        ".sub-title { font-size: 7pt; font-weight: bold; color: #15294d; margin-bottom: 3pt; }" +
                        ".tva-table { width: 85%; border-collapse: collapse; font-size: 7pt; margin-bottom: 6pt; }" +
                        ".tva-table thead th { background: #1e5aa0; color: #fff; padding: 3pt 5pt; font-weight: bold; }" +
                        ".tva-table tbody td { padding: 3pt 5pt; border-bottom: 0.5pt solid #e0e8f0; background: #f8fafd; }" +
                        ".bank-info { font-size: 7.5pt; color: #3d4a58; line-height: 1.6; background: #f0f4fa; padding: 4pt 6pt; border-left: 2pt solid #1e5aa0; margin-bottom: 6pt; }" +
                        ".payment-methods { font-size: 7pt; color: #7a8898; font-style: italic; }" +
                        ".amounts-table { width: 100%; border-collapse: collapse; font-size: 8pt; }" +
                        ".amounts-table td { padding: 3pt 4pt; color: #5a6475; }" +
                        ".amounts-table td + td { text-align: right; color: #1e2632; }" +
                        ".total-ttc-row td { font-size: 12pt; font-weight: bold; color: #15294d; padding: 5pt 4pt; border-top: 1.5pt solid #15294d; border-bottom: 1.5pt solid #15294d; }" +
                        ".amount-words { font-size: 6.5pt; color: #7a8898; font-style: italic; padding-bottom: 5pt; }" +
                        ".status-badge { font-size: 8pt; font-weight: bold; padding: 3pt 8pt; }" +
                        ".status-paid { color: #19854a; }" +
                        ".status-unpaid { color: #b42323; }" +
                        ".color-red { color: #b42323; font-weight: bold; }" +
                        ".color-green { color: #19854a; font-weight: bold; }" +
                        ".bold-val { font-weight: bold; }" +
                        ".footer-divider { border-top: 0.5pt solid #c8d4e6; margin: 6pt 0; }" +
                        ".footer-grid { width: 100%; border-collapse: collapse; }" +
                        ".footer-col { width: 33%; vertical-align: top; padding-right: 8pt; font-size: 7pt; }" +
                        ".footer-text { color: #7a8898; line-height: 1.6; margin-top: 3pt; }" +
                        ".footer-legal { color: #9aa3ae; line-height: 1.7; text-align: right; padding-right: 0; }" +
                        ".signature-col { text-align: center; }" +
                        ".signature-box { border: 0.5pt solid #c8d4e6; height: 32pt; color: #aab4be; font-style: italic; font-size: 6.5pt; text-align: center; margin-top: 3pt; padding-top: 24pt; }" +
                        ".footer-banner { background: #15294d; color: #ffffff; text-align: center; font-size: 7.5pt; font-style: italic; padding: 5pt; margin-top: 7pt; }" +
                        ".center { text-align: center; }" +
                        ".right { text-align: right; }" +
                        ".muted { color: #9aa3ae; }" +
                        ".bold { font-weight: bold; }";
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════════════════

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
            InputStream is = getClass().getClassLoader().getResourceAsStream(companyLogoPath);
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                String b64  = Base64.getEncoder().encodeToString(bytes);
                String mime = companyLogoPath.endsWith(".png") ? "image/png" : "image/jpeg";
                return "<img class=\"logo-img\" src=\"data:" + mime + ";base64," + b64 + "\"/>";
            }
        } catch (IOException e) {
            log.debug("Logo non trouvé: {}", companyLogoPath);
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

    private String nvl(String s) { return s != null ? esc(s) : "&#8212;"; }

    /**
     * FIX CENTRAL — Échappe TOUTES les données dynamiques avant injection HTML.
     * Règle absolue : appeler esc() sur TOUT ce qui vient de la base de données.
     * Le & DOIT être remplacé en premier pour éviter le double-échappement.
     */
    protected String esc(String s) {
        if (s == null) return "";
        return s.replace("&",  "&amp;")   // EN PREMIER obligatoirement
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }

    private String safeRef() {
        String n = order.getOrderNumber();
        return esc(n.length() > 4 ? n.substring(4) : n);
    }
}