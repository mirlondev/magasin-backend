package org.odema.posnew.design.builder.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Builder pour les tickets d'annulation de commande.
 * Format thermique 80mm - Style distinct avec bandeau barré.
 */
@Slf4j
@Component
public class CancellationDocumentBuilder extends AbstractPdfDocumentBuilder {

    private static final DateTimeFormatter FMT_DT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private String companyName        = "ODEMA POS";
    private String companyAddress     = "123 Rue Principale";
    private String companyPhone       = "+237 6XX XX XX XX";
    private String companyTaxId       = "TAX-123456";
    private String footerMessage      = "Cette commande a &#233;t&#233; annul&#233;e";
    private String logoPath           = "static/logo.png";

    private String cancellationReason;
    private String cancelledBy;
    private final StringBuilder html = new StringBuilder();

    public CancellationDocumentBuilder() { super(null); }

    public CancellationDocumentBuilder(Order order) {
        super(order);
    }

    public CancellationDocumentBuilder withConfig(String companyName, String companyAddress,
                                                  String companyPhone, String companyTaxId,
                                                  String footerMessage) {
        if (companyName    != null && !companyName.isBlank()) this.companyName    = companyName;
        if (companyAddress != null) this.companyAddress = companyAddress;
        if (companyPhone   != null) this.companyPhone   = companyPhone;
        if (companyTaxId   != null) this.companyTaxId   = companyTaxId;
        if (footerMessage  != null) this.footerMessage  = footerMessage;
        return this;
    }

    public CancellationDocumentBuilder withCancellationReason(String reason) {
        this.cancellationReason = reason;
        return this;
    }

    public CancellationDocumentBuilder withCancelledBy(String username) {
        this.cancelledBy = username;
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
        String logoTag = buildLogoTag();

        // Bandeau d'annulation barré
        html.append("<div class=\"cancel-banner\">");
        html.append("<span class=\"cancel-strike\">&#10008;</span> ");
        html.append("ANNUL&#201;");
        html.append(" <span class=\"cancel-strike\">&#10008;</span>");
        html.append("</div>");

        html.append("<div class=\"header\">")
                .append(logoTag.isEmpty() ? "" : "<div class=\"logo-wrap\">" + logoTag + "</div>")
                .append("<div class=\"company-name\">").append(esc(companyName)).append("</div>");
        if (!companyAddress.isBlank())
            html.append("<div class=\"company-info\">").append(esc(companyAddress)).append("</div>");
        if (!companyPhone.isBlank())
            html.append("<div class=\"company-info\">T&#233;l: ").append(esc(companyPhone)).append("</div>");
        if (!companyTaxId.isBlank())
            html.append("<div class=\"company-info\">NIF: ").append(esc(companyTaxId)).append("</div>");
        html.append("</div>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addMainInfo() {
        html.append("<div class=\"doc-type\">TICKET D'ANNULATION</div>")
                .append("<table class=\"info-table\">")
                .append(infoRow("N&#176; Commande", order != null ? esc(order.getOrderNumber()) : "&#8212;"))
                .append(infoRow("Date annulation", order != null && order.getUpdatedAt() != null
                        ? order.getUpdatedAt().format(FMT_DT) : "&#8212;"))
                .append(infoRow("Date origine", order != null && order.getCreatedAt() != null
                        ? order.getCreatedAt().format(FMT_DT) : "&#8212;"))
                .append(infoRow("Caisse", order != null && order.getStore() != null
                        ? esc(order.getStore().getName()) : "&#8212;"))
                .append(infoRow("Caissier orig.", order != null && order.getCashier() != null
                        ? esc(order.getCashier().getUsername()) : "&#8212;"));

        if (cancelledBy != null && !cancelledBy.isBlank()) {
            html.append(infoRow("Annul&#233; par", esc(cancelledBy)));
        }

        if (order != null && order.getCustomer() != null) {
            html.append(infoRow("Client", esc(order.getCustomer().getFullName())));
        }

        // Raison d'annulation
        if (cancellationReason != null && !cancellationReason.isBlank()) {
            html.append("<tr><td colspan=\"2\" class=\"reason-label\">Motif d'annulation:</td></tr>")
                    .append("<tr><td colspan=\"2\" class=\"reason-box\">")
                    .append(esc(cancellationReason)).append("</td></tr>");
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addItemsTable() {
        html.append("<div class=\"items-title\">ARTICLES ANNUL&#201;S</div>")
                .append("<table class=\"items-table\">")
                .append("<thead><tr>")
                .append("<th class=\"col-name\">Article</th>")
                .append("<th class=\"col-qty\">Qt&#233;</th>")
                .append("<th class=\"col-pu\">P.U.</th>")
                .append("<th class=\"col-ttl\">Total</th>")
                .append("</tr></thead><tbody>");

        if (order != null && order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String name = item.getProduct() != null ? item.getProduct().getName() : "Article";
                if (name.length() > 20) name = name.substring(0, 18) + "..";

                html.append("<tr class=\"cancelled-row\">")
                        .append("<td class=\"col-name\"><span class=\"strike\">").append(esc(name)).append("</span></td>")
                        .append("<td class=\"center\"><span class=\"strike\">").append(item.getQuantity()).append("</span></td>")
                        .append("<td class=\"right\"><span class=\"strike\">").append(fmtCur(item.getUnitPrice())).append("</span></td>")
                        .append("<td class=\"right\"><span class=\"strike\">").append(fmtCur(item.getFinalPrice())).append("</span></td>")
                        .append("</tr>");
            }
        }

        html.append("</tbody></table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addTotals() {
        html.append("<table class=\"totals-table\">");

        if (order != null) {
            html.append(totalRow("Sous-total HT", fmtCur(order.getSubtotal()), "strike"));

            if (order.getTaxAmount() != null && order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
                String lbl = "TVA (" + (order.getTaxRate() != null ? order.getTaxRate() : "18") + "%)";
                html.append(totalRow(lbl, fmtCur(order.getTaxAmount()), "strike"));
            }

            if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
                html.append(totalRow("Remise", "-" + fmtCur(order.getDiscountAmount()), "strike color-muted"));

            html.append("<tr class=\"total-sep\"><td colspan=\"2\">&#160;</td></tr>")
                    .append("<tr class=\"total-cancelled\">")
                    .append("<td>TOTAL ANNUL&#201;</td>")
                    .append("<td class=\"right strike\">").append(fmtCur(order.getTotalAmount())).append("</td>")
                    .append("</tr>");

            // Statut de remboursement si applicable
            if (order.getTotalPaid() != null && order.getTotalPaid().compareTo(BigDecimal.ZERO) > 0) {
                html.append("<tr class=\"sep-spacer\"><td colspan=\"2\">&#160;</td></tr>")
                        .append(totalRow("D&#233;j&#224; pay&#233;", fmtCur(order.getTotalPaid()), "color-orange"))
                        .append(totalRow("&#192; rembourser", fmtCur(order.getTotalPaid()), "color-red bold"));
            }
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addFooter() {
        html.append("<div class=\"footer-msg\">").append(esc(footerMessage)).append("</div>");

        // Avertissement
        html.append("<div class=\"warning-box\">")
                .append("&#9888; Cette annulation est d&#233;finitive et irr&#233;versible.<br/>")
                .append("Les stocks ont &#233;t&#233; r&#233;approvisionn&#233;s.")
                .append("</div>");

        if (order != null && order.getOrderNumber() != null) {
            html.append("<div class=\"barcode-line\">* ").append(esc(order.getOrderNumber())).append("-ANNUL&#201; *</div>");
        }

        // Signatures
        html.append("<div class=\"signatures\">")
                .append("<div class=\"sig-col\">")
                .append("<div class=\"sig-label\">Caissier</div>")
                .append("<div class=\"sig-box\">&#160;</div>")
                .append("</div>")
                .append("<div class=\"sig-col\">")
                .append("<div class=\"sig-label\">Manager</div>")
                .append("<div class=\"sig-box\">&#160;</div>")
                .append("</div>")
                .append("</div>");

        html.append("<div class=\"legal-text\">")
                .append("Document g&#233;n&#233;r&#233; le ")
                .append(java.time.LocalDateTime.now().format(FMT_DT))
                .append("<br/>Conservez cet original pour vos archives.")
                .append("</div>")

                .append("<div class=\"cut-line\">- - - - - - &#9988; - - - - - -</div>")
                .append("</body></html>");
        return this;
    }

    @Override
    public byte[] build() {
        try {
            ByteArrayOutputStream baos = (ByteArrayOutputStream) this.outputStream;
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html.toString(), null);
            builder.toStream(baos);
            builder.run();
            log.info("Ticket annulation PDF g&#233;n&#233;r&#233; ({} octets)", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur g&#233;n&#233;ration ticket annulation PDF: {}", e.getMessage());
            throw new RuntimeException("&#201;chec g&#233;n&#233;ration ticket annulation PDF", e);
        }
    }

    private String buildCss() {
        return
                "@page { size: 80mm auto; margin: 4mm 3mm; }" +
                        "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                        "body { font-family: 'Courier New', Courier, monospace; font-size: 8pt; color: #1a1a1a; line-height: 1.3; width: 74mm; }" +

                        // Bandeau annulation
                        ".cancel-banner { background: #555; color: #ffffff; text-align: center; font-size: 12pt; font-weight: bold; padding: 5pt; margin-bottom: 4pt; letter-spacing: 2pt; }" +
                        ".cancel-strike { color: #ff6b6b; font-size: 14pt; }" +

                        // Header
                        ".header { text-align: center; padding-bottom: 4pt; }" +
                        ".logo-wrap img { max-width: 48pt; max-height: 36pt; margin-bottom: 3pt; }" +
                        ".company-name { font-size: 13pt; font-weight: bold; letter-spacing: 1pt; margin-bottom: 2pt; }" +
                        ".company-info { font-size: 7pt; color: #444; line-height: 1.4; }" +

                        // Séparateurs
                        ".sep-dashed { border-top: 1pt dashed #999; margin: 4pt 0; font-size: 1pt; color: transparent; }" +

                        // Type document
                        ".doc-type { text-align: center; font-size: 9pt; font-weight: bold; letter-spacing: 1pt; padding: 3pt 0; color: #555; }" +

                        // Table infos
                        ".info-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".info-table td { padding: 1pt 2pt; vertical-align: top; }" +
                        ".info-table td:first-child { font-weight: bold; width: 38%; color: #333; white-space: nowrap; }" +
                        ".reason-label { font-weight: bold; color: #b42020; padding-top: 4pt; }" +
                        ".reason-box { background: #fff5f5; border-left: 2pt solid #b42020; padding: 3pt 5pt; margin: 2pt 0; font-style: italic; color: #555; }" +

                        // Articles
                        ".items-title { text-align: center; font-size: 7pt; font-weight: bold; color: #777; text-transform: uppercase; letter-spacing: 0.5pt; margin-bottom: 3pt; }" +
                        ".items-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; margin: 2pt 0; }" +
                        ".items-table thead tr { border-bottom: 1pt solid #777; }" +
                        ".items-table thead th { font-size: 7pt; font-weight: bold; padding: 2pt; text-transform: uppercase; color: #777; }" +
                        ".items-table tbody td { padding: 2.5pt 2pt; border-bottom: 0.5pt dotted #ccc; }" +
                        ".cancelled-row { background: #f9f9f9; }" +
                        ".strike { text-decoration: line-through; color: #888; }" +
                        ".col-name { width: 42%; text-align: left; }" +
                        ".col-qty { width: 12%; text-align: center; }" +
                        ".col-pu { width: 22%; text-align: right; }" +
                        ".col-ttl { width: 24%; text-align: right; }" +

                        // Table totaux
                        ".totals-table { width: 100%; border-collapse: collapse; font-size: 8pt; margin: 2pt 0; }" +
                        ".totals-table td { padding: 1.5pt 2pt; }" +
                        ".totals-table td + td { text-align: right; }" +
                        ".total-sep td { border-top: 1pt solid #777; padding: 2pt 0 0; font-size: 2pt; color: transparent; }" +
                        ".sep-spacer td { height: 4pt; font-size: 2pt; color: transparent; }" +
                        ".total-cancelled td { font-size: 11pt; font-weight: bold; color: #555; padding: 3pt 2pt; }" +

                        // Footer
                        ".footer-msg { text-align: center; font-size: 8pt; font-style: italic; padding: 4pt 0 2pt; color: #b42020; }" +
                        ".warning-box { background: #fff3cd; border: 0.5pt solid #ffc107; color: #856404; font-size: 7pt; padding: 4pt; margin: 4pt 0; text-align: center; line-height: 1.4; }" +
                        ".barcode-line { text-align: center; font-size: 9pt; letter-spacing: 1pt; padding: 2pt 0; color: #777; }" +

                        // Signatures
                        ".signatures { display: table; width: 100%; margin-top: 6pt; }" +
                        ".sig-col { display: table-cell; width: 50%; text-align: center; padding: 0 4pt; }" +
                        ".sig-label { font-size: 7pt; color: #555; margin-bottom: 2pt; }" +
                        ".sig-box { border-top: 0.5pt solid #999; height: 18pt; }" +

                        // Légal
                        ".legal-text { text-align: center; font-size: 6.5pt; color: #666; line-height: 1.5; padding: 4pt 0; }" +

                        // Ligne de coupe
                        ".cut-line { text-align: center; font-size: 8pt; color: #aaa; margin-top: 6pt; letter-spacing: 1pt; }" +

                        // Utilitaires
                        ".center { text-align: center; }" +
                        ".right { text-align: right; }" +
                        ".bold { font-weight: bold; }" +
                        ".color-red { color: #b42020; }" +
                        ".color-orange { color: #e67e22; }" +
                        ".color-muted { color: #888; }";
    }

    private String infoRow(String label, String value) {
        return "<tr><td>" + label + ":</td><td>" + value + "</td></tr>";
    }

    private String totalRow(String label, String value, String css) {
        String tdCss = css.isEmpty() ? "" : " class=\"" + css + "\"";
        String valCss = "right" + (css.isEmpty() ? "" : " " + css);
        return "<tr><td" + tdCss + ">" + label + "</td><td class=\"" + valCss + "\">" + value + "</td></tr>";
    }

    private String buildLogoTag() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(logoPath);
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                String b64  = Base64.getEncoder().encodeToString(bytes);
                String mime = logoPath.endsWith(".png") ? "image/png" : "image/jpeg";
                return "<img src=\"data:" + mime + ";base64," + b64 + "\"/>";
            }
        } catch (IOException e) {
            log.debug("Logo annulation non trouv&#233;: {}", logoPath);
        }
        return "";
    }

    private String fmtCur(BigDecimal amount) {
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
