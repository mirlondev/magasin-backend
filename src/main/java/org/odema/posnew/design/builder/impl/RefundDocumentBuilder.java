package org.odema.posnew.design.builder.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.OrderItem;
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
 * Builder pour les tickets de remboursement.
 * Format thermique 80mm - Style distinct avec bandeau rouge.
 */
@Slf4j
@Component
public class RefundDocumentBuilder extends AbstractPdfDocumentBuilder {

    private static final DateTimeFormatter FMT_DT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private String companyName        = "ODEMA POS";
    private String companyAddress     = "123 Rue Principale";
    private String companyPhone       = "+237 6XX XX XX XX";
    private String companyTaxId       = "TAX-123456";
    private String footerMessage      = "Merci de votre confiance !";
    private String logoPath           = "static/logo.png";

    private Refund refund;
    private Order originalOrder;
    private final StringBuilder html = new StringBuilder();

    public RefundDocumentBuilder() { super(null); }

    public RefundDocumentBuilder(Refund refund, Order originalOrder) {
        super(null);
        this.refund = refund;
        this.originalOrder = originalOrder;
    }

    public RefundDocumentBuilder withConfig(String companyName, String companyAddress,
                                            String companyPhone, String companyTaxId,
                                            String footerMessage) {
        if (companyName    != null && !companyName.isBlank()) this.companyName    = companyName;
        if (companyAddress != null) this.companyAddress = companyAddress;
        if (companyPhone   != null) this.companyPhone   = companyPhone;
        if (companyTaxId   != null) this.companyTaxId   = companyTaxId;
        if (footerMessage  != null) this.footerMessage  = footerMessage;
        return this;
    }

    public RefundDocumentBuilder withRefund(Refund refund) {
        this.refund = refund;
        return this;
    }

    public RefundDocumentBuilder withOriginalOrder(Order order) {
        this.originalOrder = order;
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

        // Bandeau rouge pour remboursement
        html.append("<div class=\"refund-banner\">&#8617; REMBOURSEMENT &#8617;</div>");

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
        html.append("<div class=\"doc-type\">TICKET DE REMBOURSEMENT</div>")
                .append("<table class=\"info-table\">")
                .append(infoRow("N&#176; Remb.", refund != null ? esc(refund.getRefundNumber()) : "&#8212;"))
                .append(infoRow("Date", refund != null && refund.getCreatedAt() != null
                        ? refund.getCompletedAt().format(FMT_DT) : "&#8212;"))
                .append(infoRow("Caisse", originalOrder != null && originalOrder.getStore() != null
                        ? esc(originalOrder.getStore().getName()) : "&#8212;"))
                .append(infoRow("Caissier", originalOrder != null && originalOrder.getCashier() != null
                        ? esc(originalOrder.getCashier().getUsername()) : "&#8212;"));

        if (originalOrder != null) {
            html.append(infoRow("N&#176; Vente", esc(originalOrder.getOrderNumber())));
        }

        if (originalOrder != null && originalOrder.getCustomer() != null) {
            html.append(infoRow("Client", esc(originalOrder.getCustomer().getFullName())));
            if (originalOrder.getCustomer().getPhone() != null)
                html.append(infoRow("T&#233;l", esc(originalOrder.getCustomer().getPhone())));
        }

        // Raison du remboursement
        if (refund != null && refund.getReason() != null && !refund.getReason().isBlank()) {
            html.append("<tr><td colspan=\"2\" class=\"reason-label\">Motif:</td></tr>")
                    .append("<tr><td colspan=\"2\" class=\"reason-value\">")
                    .append(esc(refund.getReason())).append("</td></tr>");
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addItemsTable() {
        html.append("<table class=\"items-table\">")
                .append("<thead><tr>")
                .append("<th class=\"col-name\">Article rembours&#233;</th>")
                .append("<th class=\"col-qty\">Qt&#233;</th>")
                .append("<th class=\"col-ttl\">Montant</th>")
                .append("</tr></thead><tbody>");

        if (refund != null && refund.getItems() != null) {
            for (RefundItem item : refund.getItems()) {
                String name = item.getProduct() != null ? item.getProduct().getName() : "Article";
                if (name.length() > 25) name = name.substring(0, 23) + "..";

                html.append("<tr>")
                        .append("<td class=\"col-name\">").append(esc(name)).append("</td>")
                        .append("<td class=\"center\">").append(item.getQuantity()).append("</td>")
                        .append("<td class=\"right bold color-red\">-")
                        .append(fmtCur(item.getRefundAmount())).append("</td>")
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

        if (refund != null) {
            // Sous-total des articles remboursés
            BigDecimal itemsTotal = refund.getItems().stream()
                    .map(RefundItem::getRefundAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            html.append(totalRow("Articles rembours&#233;s", "-" + fmtCur(itemsTotal), "color-red"));

            // Frais de restockage si applicable
            if (refund.getRestockingFee() != null && refund.getRestockingFee().compareTo(BigDecimal.ZERO) > 0) {
                html.append(totalRow("Frais de restockage", fmtCur(refund.getRestockingFee()), ""));
            }

            html.append("<tr class=\"total-sep\"><td colspan=\"2\">&#160;</td></tr>")
                    .append("<tr class=\"total-refund\">")
                    .append("<td>TOTAL REMBOURS&#201;</td>")
                    .append("<td class=\"right\">").append(fmtCur(refund.getTotalRefundAmount())).append("</td>")
                    .append("</tr>");

            // Mode de remboursement
            if (refund.getRefundMethod() != null) {
                html.append("<tr class=\"sep-spacer\"><td colspan=\"2\">&#160;</td></tr>")
                        .append(totalRow("Mode", fmtRefundMethod(refund.getRefundMethod()), "color-muted"));
            }
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addFooter() {
        html.append("<div class=\"footer-msg\">").append(esc(footerMessage)).append("</div>");

        if (refund != null && refund.getRefundNumber() != null) {
            html.append("<div class=\"barcode-line\">* ").append(esc(refund.getRefundNumber())).append(" *</div>");
        }

        html.append("<div class=\"legal-text\">")
                .append("Ce remboursement a &#233;t&#233; trait&#233; conform&#233;ment &#224; notre politique de retour.<br/>")
                .append("D&#233;lai de traitement: 3 &#224; 5 jours ouvr&#233;s.<br/>")
                .append("Conservez ce ticket pour suivi.")
                .append("</div>")

                // Signature
                .append("<div class=\"signature-section\">")
                .append("<div class=\"signature-line\">Signature client</div>")
                .append("<div class=\"signature-box\">&#160;</div>")
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
            log.info("Ticket remboursement PDF g&#233;n&#233;r&#233; ({} octets)", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur g&#233;n&#233;ration ticket remboursement PDF: {}", e.getMessage());
            throw new RuntimeException("&#201;chec g&#233;n&#233;ration ticket remboursement PDF", e);
        }
    }

    private String buildCss() {
        return
                "@page { size: 80mm auto; margin: 4mm 3mm; }" +
                        "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                        "body { font-family: 'Courier New', Courier, monospace; font-size: 8pt; color: #1a1a1a; line-height: 1.3; width: 74mm; }" +

                        // Bandeau remboursement
                        ".refund-banner { background: #b42020; color: #ffffff; text-align: center; font-size: 10pt; font-weight: bold; padding: 4pt; margin-bottom: 4pt; letter-spacing: 1pt; }" +

                        // Header
                        ".header { text-align: center; padding-bottom: 4pt; }" +
                        ".logo-wrap img { max-width: 48pt; max-height: 36pt; margin-bottom: 3pt; }" +
                        ".company-name { font-size: 13pt; font-weight: bold; letter-spacing: 1pt; margin-bottom: 2pt; }" +
                        ".company-info { font-size: 7pt; color: #444; line-height: 1.4; }" +

                        // Séparateurs
                        ".sep-dashed { border-top: 1pt dashed #999; margin: 4pt 0; font-size: 1pt; color: transparent; }" +

                        // Type document
                        ".doc-type { text-align: center; font-size: 9pt; font-weight: bold; letter-spacing: 1pt; padding: 3pt 0; color: #b42020; }" +

                        // Table infos
                        ".info-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".info-table td { padding: 1pt 2pt; vertical-align: top; }" +
                        ".info-table td:first-child { font-weight: bold; width: 34%; color: #333; white-space: nowrap; }" +
                        ".reason-label { font-weight: bold; color: #b42020; padding-top: 3pt; }" +
                        ".reason-value { font-style: italic; color: #555; padding-bottom: 2pt; }" +

                        // Table articles
                        ".items-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; margin: 2pt 0; }" +
                        ".items-table thead tr { border-bottom: 1pt solid #b42020; }" +
                        ".items-table thead th { font-size: 7pt; font-weight: bold; padding: 2pt; text-transform: uppercase; color: #b42020; }" +
                        ".items-table tbody td { padding: 2.5pt 2pt; border-bottom: 0.5pt dotted #ccc; }" +
                        ".col-name { width: 55%; text-align: left; }" +
                        ".col-qty { width: 15%; text-align: center; }" +
                        ".col-ttl { width: 30%; text-align: right; }" +

                        // Table totaux
                        ".totals-table { width: 100%; border-collapse: collapse; font-size: 8pt; margin: 2pt 0; }" +
                        ".totals-table td { padding: 1.5pt 2pt; }" +
                        ".totals-table td + td { text-align: right; }" +
                        ".total-sep td { border-top: 1pt solid #b42020; padding: 2pt 0 0; font-size: 2pt; color: transparent; }" +
                        ".sep-spacer td { height: 4pt; font-size: 2pt; color: transparent; }" +
                        ".total-refund td { font-size: 11pt; font-weight: bold; color: #b42020; padding: 3pt 2pt; }" +

                        // Footer
                        ".footer-msg { text-align: center; font-size: 8pt; font-style: italic; padding: 4pt 0 2pt; color: #333; }" +
                        ".barcode-line { text-align: center; font-size: 9pt; letter-spacing: 1pt; padding: 2pt 0; }" +
                        ".legal-text { text-align: center; font-size: 6.5pt; color: #666; line-height: 1.5; padding: 2pt 0; }" +

                        // Signature
                        ".signature-section { margin-top: 6pt; text-align: center; }" +
                        ".signature-line { font-size: 7pt; color: #555; margin-bottom: 2pt; }" +
                        ".signature-box { border-top: 0.5pt solid #999; height: 20pt; margin: 0 20pt; }" +

                        // Ligne de coupe
                        ".cut-line { text-align: center; font-size: 8pt; color: #aaa; margin-top: 6pt; letter-spacing: 1pt; }" +

                        // Utilitaires
                        ".center { text-align: center; }" +
                        ".right { text-align: right; }" +
                        ".bold { font-weight: bold; }" +
                        ".color-red { color: #b42020; }" +
                        ".color-muted { color: #555; }";
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
            log.debug("Logo remboursement non trouv&#233;: {}", logoPath);
        }
        return "";
    }

    private String fmtCur(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.0f FCFA", amount);
    }

    private String fmtRefundMethod(org.odema.posnew.entity.enums.RefundMethod method) {
        if (method == null) return "M&#234;me mode de paiement";
        return switch (method) {
            case CASH -> "Esp&#232;ces";
            case CREDIT_CARD -> "Carte bancaire";
            case MOBILE_MONEY -> "Mobile Money";
            case BANK_TRANSFER -> "Virement bancaire";
            case STORE_CREDIT -> "Avoir magasin";
            case ORIGINAL_PAYMENT_METHOD -> "M&#234;me mode de paiement";
            default -> esc(method.name());
        };
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
