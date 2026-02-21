package org.odema.posnew.design.builder.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;

import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.OrderItem;
import org.odema.posnew.domain.model.Payment;
import org.odema.posnew.domain.model.enums.PaymentMethod;
import org.odema.posnew.domain.model.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Slf4j
@Component
public class ReceiptDocumentBuilder extends AbstractPdfDocumentBuilder {

    private static final DateTimeFormatter FMT_DT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private String rcptCompanyName    = "ODEMA POS";
    private String rcptCompanyAddress = "123 Rue Principale";
    private String rcptCompanyPhone   = "+237 6XX XX XX XX";
    private String rcptCompanyTaxId   = "TAX-123456";
    private String rcptFooterMessage  = "Merci de votre visite !";
    private String rcptLogoPath       = "static/logo.png";

    private final StringBuilder html = new StringBuilder();

    public ReceiptDocumentBuilder() { super(null); }
    public ReceiptDocumentBuilder(Order order) { super(order); }

    public ReceiptDocumentBuilder withConfig(String companyName, String companyAddress,
                                             String companyPhone, String companyTaxId,
                                             String footerMessage) {
        if (companyName    != null && !companyName.isBlank()) this.rcptCompanyName    = companyName;
        if (companyAddress != null) this.rcptCompanyAddress = companyAddress;
        if (companyPhone   != null) this.rcptCompanyPhone   = companyPhone;
        if (companyTaxId   != null) this.rcptCompanyTaxId   = companyTaxId;
        if (footerMessage  != null) this.rcptFooterMessage  = footerMessage;
        return this;
    }

    @Override
    public DocumentBuilder initialize() {
        this.outputStream = new ByteArrayOutputStream();
        html.setLength(0);
        // FIX: DOCTYPE XHTML strict — obligatoire pour openhtmltopdf
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
        html.append("<div class=\"header\">")
                .append(logoTag.isEmpty() ? "" : "<div class=\"logo-wrap\">" + logoTag + "</div>")
                .append("<div class=\"company-name\">").append(esc(rcptCompanyName)).append("</div>");
        if (!rcptCompanyAddress.isBlank())
            html.append("<div class=\"company-info\">").append(esc(rcptCompanyAddress)).append("</div>");
        if (!rcptCompanyPhone.isBlank())
            html.append("<div class=\"company-info\">T&#233;l: ").append(esc(rcptCompanyPhone)).append("</div>");
        if (!rcptCompanyTaxId.isBlank())
            html.append("<div class=\"company-info\">NIF: ").append(esc(rcptCompanyTaxId)).append("</div>");
        html.append("</div>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addMainInfo() {
        html.append("<div class=\"doc-type\">TICKET DE CAISSE</div>")
                .append("<table class=\"info-table\">")
                .append(infoRow("Ticket",    esc(order.getOrderNumber())))
                .append(infoRow("Date",      order.getCreatedAt() != null
                        ? order.getCreatedAt().format(FMT_DT) : "&#8212;"))
                .append(infoRow("Caisse",    order.getStore()   != null ? esc(order.getStore().getName())       : "&#8212;"))
                .append(infoRow("Caissier",  order.getCashier() != null ? esc(order.getCashier().getUsername()) : "&#8212;"));

        if (order.getCustomer() != null) {
            html.append(infoRow("Client", esc(order.getCustomer().getFullName())));
            if (order.getCustomer().getPhone() != null)
                html.append(infoRow("T&#233;l", esc(order.getCustomer().getPhone())));
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addItemsTable() {
        html.append("<table class=\"items-table\">")
                .append("<thead><tr>")
                .append("<th class=\"col-name\">Article</th>")
                .append("<th class=\"col-qty\">Qt&#233;</th>")
                .append("<th class=\"col-pu\">P.U.</th>")
                .append("<th class=\"col-ttl\">Total</th>")
                .append("</tr></thead><tbody>");

        for (OrderItem item : order.getItems()) {
            String name = item.getProduct() != null ? item.getProduct().getName() : "Article";
            if (name.length() > 22) name = name.substring(0, 20) + "..";

            html.append("<tr>")
                    .append("<td class=\"col-name\">").append(esc(name)).append("</td>")
                    .append("<td class=\"center\">").append(item.getQuantity()).append("</td>")
                    .append("<td class=\"right\">").append(fmtCur(item.getUnitPrice())).append("</td>")
                    .append("<td class=\"right bold\">").append(fmtCur(item.getFinalPrice())).append("</td>")
                    .append("</tr>");
        }

        html.append("</tbody></table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addTotals() {
        html.append("<table class=\"totals-table\">");

        if (order.getSubtotal() != null)
            html.append(totalRow("Sous-total HT", fmtCur(order.getSubtotal()), ""));

//        if (order.getTaxAmount() != null && order.getTaxAmount().compareTo(BigDecimal.ZERO) > 0) {
//            String lbl = "TVA (" + (order.getTaxRate() != null ? order.getTaxRate() : "18") + "%)";
//            html.append(totalRow(lbl, fmtCur(order.getTaxAmount()), ""));
//        }

        if (order.getGlobalDiscount() != null && order.getGlobalDiscountAmount().compareTo(BigDecimal.ZERO) > 0)
            html.append(totalRow("Remise", "-" + fmtCur(order.getGlobalDiscountAmount()), "color-red"));

        html.append("<tr class=\"total-sep\"><td colspan=\"2\">&#160;</td></tr>")
                .append("<tr class=\"total-ttc\">")
                .append("<td>TOTAL TTC</td>")
                .append("<td class=\"right\">").append(fmtCur(order.getTotalAmount())).append("</td>")
                .append("</tr>");

        BigDecimal totalPaid = order.getTotalPaid();
        if (totalPaid != null && totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            html.append("<tr class=\"sep-spacer\"><td colspan=\"2\">&#160;</td></tr>");

            for (Payment p : order.getPayments()) {
                if (p.getStatus() == PaymentStatus.PAID) {
                    html.append(totalRow(fmtMethod(p.getMethod()), fmtCur(p.getAmount()), "color-muted"));
                }
            }

            if (order.getChangeAmount() != null && order.getChangeAmount().compareTo(BigDecimal.ZERO) > 0)
                html.append(totalRow("Monnaie rendue", fmtCur(order.getChangeAmount()), "color-green"));

            BigDecimal remaining = order.getTotalAmount().subtract(totalPaid);
            if (remaining.compareTo(BigDecimal.ZERO) > 0)
                html.append(totalRow("Reste &#224; payer", fmtCur(remaining), "color-red bold-lbl"));
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addFooter() {
        html.append("<div class=\"footer-msg\">").append(esc(rcptFooterMessage)).append("</div>");

        if (order.getOrderNumber() != null) {
            html.append("<div class=\"barcode-line\">* ").append(esc(order.getOrderNumber())).append(" *</div>");
        }

        html.append("<div class=\"legal-text\">")
                .append("Conservez ce ticket pour tout &#233;change sous 7 jours<br/>")
                .append("TVA incluse dans le prix &#8212; Merci de votre confiance")
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
            log.info("Ticket PDF généré ({} octets)", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur génération ticket PDF: {}", e.getMessage());
            throw new RuntimeException("Échec génération ticket PDF", e);
        }
    }

    private String buildCss() {
        return
                "@page { size: 80mm auto; margin: 4mm 3mm; }" +
                        "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                        "body { font-family: 'Courier New', Courier, monospace; font-size: 8pt; color: #1a1a1a; line-height: 1.3; width: 74mm; }" +
                        ".header { text-align: center; padding-bottom: 4pt; }" +
                        ".logo-wrap img { max-width: 48pt; max-height: 36pt; margin-bottom: 3pt; }" +
                        ".company-name { font-size: 13pt; font-weight: bold; letter-spacing: 1pt; margin-bottom: 2pt; }" +
                        ".company-info { font-size: 7pt; color: #444; line-height: 1.4; }" +
                        ".sep-dashed { border-top: 1pt dashed #999; margin: 4pt 0; font-size: 1pt; color: transparent; }" +
                        ".doc-type { text-align: center; font-size: 9pt; font-weight: bold; letter-spacing: 1pt; padding: 3pt 0; }" +
                        ".info-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".info-table td { padding: 1pt 2pt; vertical-align: top; }" +
                        ".info-table td:first-child { font-weight: bold; width: 34%; color: #333; white-space: nowrap; }" +
                        ".items-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; margin: 2pt 0; }" +
                        ".items-table thead tr { border-bottom: 1pt solid #333; }" +
                        ".items-table thead th { font-size: 7pt; font-weight: bold; padding: 2pt; text-transform: uppercase; }" +
                        ".items-table tbody td { padding: 2.5pt 2pt; border-bottom: 0.5pt dotted #ccc; }" +
                        ".col-name { width: 46%; text-align: left; }" +
                        ".col-qty { width: 10%; text-align: center; }" +
                        ".col-pu { width: 22%; text-align: right; }" +
                        ".col-ttl { width: 22%; text-align: right; }" +
                        ".totals-table { width: 100%; border-collapse: collapse; font-size: 8pt; margin: 2pt 0; }" +
                        ".totals-table td { padding: 1.5pt 2pt; }" +
                        ".totals-table td + td { text-align: right; }" +
                        ".total-sep td { border-top: 1pt solid #333; padding: 2pt 0 0; font-size: 2pt; color: transparent; }" +
                        ".sep-spacer td { height: 4pt; font-size: 2pt; color: transparent; }" +
                        ".total-ttc td { font-size: 11pt; font-weight: bold; padding: 3pt 2pt; }" +
                        ".footer-msg { text-align: center; font-size: 8pt; font-style: italic; padding: 4pt 0 2pt; color: #333; }" +
                        ".barcode-line { text-align: center; font-size: 9pt; letter-spacing: 1pt; padding: 2pt 0; }" +
                        ".legal-text { text-align: center; font-size: 6.5pt; color: #666; line-height: 1.5; padding: 2pt 0; }" +
                        ".cut-line { text-align: center; font-size: 8pt; color: #aaa; margin-top: 6pt; letter-spacing: 1pt; }" +
                        ".center { text-align: center; }" +
                        ".right { text-align: right; }" +
                        ".bold { font-weight: bold; }" +
                        ".bold-lbl { font-weight: bold; }" +
                        ".color-red { color: #b42020; }" +
                        ".color-green { color: #1a7a3a; font-weight: bold; }" +
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
            InputStream is = getClass().getClassLoader().getResourceAsStream(rcptLogoPath);
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                String b64  = Base64.getEncoder().encodeToString(bytes);
                String mime = rcptLogoPath.endsWith(".png") ? "image/png" : "image/jpeg";
                return "<img src=\"data:" + mime + ";base64," + b64 + "\"/>";
            }
        } catch (IOException e) {
            log.debug("Logo ticket non trouvé: {}", rcptLogoPath);
        }
        return "";
    }

    private String fmtCur(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.0f FCFA", amount);
    }

    private String fmtMethod(PaymentMethod m) {
        if (m == null) return "Esp&#232;ces";
        return switch (m) {
            case CASH          -> "Esp&#232;ces";
            case CREDIT_CARD   -> "Carte bancaire";
            case MOBILE_MONEY  -> "Mobile Money";
            case CREDIT        -> "Cr&#233;dit";
            case CHECK         -> "Ch&#232;que";
            case BANK_TRANSFER -> "Virement";
            default            -> esc(m.name());
        };
    }

    protected String esc(String s) {
        if (s == null) return "";
        return s.replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }
}