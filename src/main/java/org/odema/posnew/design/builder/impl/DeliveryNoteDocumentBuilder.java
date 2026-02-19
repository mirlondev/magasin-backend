package org.odema.posnew.design.builder.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.entity.Customer;
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
 * Builder pour les bons de livraison.
 * Format A4 professionnel - Document de suivi logistique.
 */
@Slf4j
@Component
public class DeliveryNoteDocumentBuilder extends AbstractPdfDocumentBuilder {

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DT   = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String companyName        = "ODEMA POS";
    private String companyAddress     = "123 Rue Principale, Pointe-Noire";
    private String companyPhone       = "+237 6XX XX XX XX";
    private String companyEmail       = "contact@odema.com";
    private String companyTaxId       = "TAX-123456789";
    private String companyWebsite     = "www.odema.com";
    private String logoPath           = "static/logo.png";

    private String deliveryNoteNumber;
    private String carrierName;
    private String trackingNumber;
    private String deliveryAddress;
    private String deliveryInstructions;
    private final StringBuilder html = new StringBuilder();

    public DeliveryNoteDocumentBuilder() { super(null); }

    public DeliveryNoteDocumentBuilder(Order order) {
        super(order);
    }

    public DeliveryNoteDocumentBuilder withConfig(String companyName, String companyAddress,
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

    public DeliveryNoteDocumentBuilder withDeliveryNoteNumber(String number) {
        this.deliveryNoteNumber = number;
        return this;
    }

    public DeliveryNoteDocumentBuilder withCarrier(String carrierName, String trackingNumber) {
        this.carrierName = carrierName;
        this.trackingNumber = trackingNumber;
        return this;
    }

    public DeliveryNoteDocumentBuilder withDeliveryAddress(String address) {
        this.deliveryAddress = address;
        return this;
    }

    public DeliveryNoteDocumentBuilder withDeliveryInstructions(String instructions) {
        this.deliveryInstructions = instructions;
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

        html.append("<table class=\"doc-header\"><tr>")
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
                .append("<div class=\"doc-badge\">BON DE LIVRAISON</div>")
                .append("<table class=\"meta-table\">")
                .append(metaRow("N&#176; BL", esc(deliveryNoteNumber != null ? deliveryNoteNumber : "BL-" + (order != null ? order.getOrderNumber() : "TEMP"))))
                .append(metaRow("Date", java.time.LocalDateTime.now().format(FMT_DATE)))
                .append(metaRow("R&#233;f. Commande", order != null ? esc(order.getOrderNumber()) : "&#8212;"))
                .append("</table>")
                .append("</td>")
                .append("</tr></table>");

        return this;
    }

    @Override
    public DocumentBuilder addMainInfo() {
        Customer c = order != null ? order.getCustomer() : null;

        html.append("<table class=\"info-grid\"><tr>")

                // Destinataire
                .append("<td class=\"info-box info-box--navy\">")
                .append("<div class=\"info-box__title\">DESTINATAIRE</div>")
                .append("<div class=\"info-box__name\">")
                .append(c != null ? esc(c.getFullName()) : "&#8212;")
                .append("</div>")
                .append("<div class=\"info-box__body\">");

        // Adresse de livraison (spécifique ou adresse client)
        String addr = deliveryAddress != null ? deliveryAddress :
                (c != null ? c.getAddress() : null);
        if (addr != null) {
            html.append(esc(addr)).append("<br/>");
        }
        html.append("T&#233;l: ").append(c != null && c.getPhone() != null ? esc(c.getPhone()) : "&#8212;").append("<br/>");
        if (c != null && c.getEmail() != null) {
            html.append(esc(c.getEmail()));
        }

        html.append("</div>")
                .append("</td>")

                // Informations livraison
                .append("<td class=\"info-box info-box--green\">")
                .append("<div class=\"info-box__title\">D&#201;TAILS LIVRAISON</div>")
                .append("<div class=\"info-box__body\">");

        if (carrierName != null) {
            html.append("<b>Transporteur:</b> ").append(esc(carrierName)).append("<br/>");
        }
        if (trackingNumber != null) {
            html.append("<b>N&#176; suivi:</b> ").append(esc(trackingNumber)).append("<br/>");
        }
        html.append("<b>Date pr&#233;vue:</b> ").append(java.time.LocalDateTime.now().plusDays(2).format(FMT_DATE)).append("<br/>");
        html.append("<b>Point de vente:</b> ")
                .append(order != null && order.getStore() != null ? esc(order.getStore().getName()) : "&#8212;");

        html.append("</div>")
                .append("</td>")
                .append("</tr></table>");

        // Instructions spéciales
        if (deliveryInstructions != null && !deliveryInstructions.isBlank()) {
            html.append("<div class=\"instructions-box\">")
                    .append("<div class=\"instructions-title\">&#128712; INSTRUCTIONS SP&#201;CIALES</div>")
                    .append("<div class=\"instructions-body\">").append(esc(deliveryInstructions)).append("</div>")
                    .append("</div>");
        }

        return this;
    }

    @Override
    public DocumentBuilder addItemsTable() {
        html.append("<div class=\"section-title\">ARTICLES &#192; LIVRER</div>")
                .append("<table class=\"items-table\">")
                .append("<thead><tr>")
                .append("<th class=\"col-num\">N&#176;</th>")
                .append("<th class=\"col-desc\">D&#201;SIGNATION</th>")
                .append("<th class=\"col-sku\">R&#201;F&#201;RENCE</th>")
                .append("<th class=\"col-qty\">QT&#201;</th>")
                .append("<th class=\"col-check\">&#10003;</th>")
                .append("</tr></thead><tbody>");

        if (order != null && order.getItems() != null) {
            int i = 1;
            for (OrderItem item : order.getItems()) {
                String rowClass = (i % 2 == 0) ? "row-alt" : "";
                String name = item.getProduct() != null ? esc(item.getProduct().getName()) : "Article";
                String sku = item.getProduct() != null && item.getProduct().getSku() != null
                        ? esc(item.getProduct().getSku()) : "&#8212;";

                html.append("<tr class=\"").append(rowClass).append("\">")
                        .append("<td class=\"center\">").append(i).append("</td>")
                        .append("<td>").append(name).append("</td>")
                        .append("<td class=\"center sku\">").append(sku).append("</td>")
                        .append("<td class=\"center bold\">").append(item.getQuantity()).append("</td>")
                        .append("<td class=\"center check-box\">&#9744;</td>")
                        .append("</tr>");
                i++;
            }
        }

        int totalQty = order != null ? order.getItems().stream().mapToInt(OrderItem::getQuantity).sum() : 0;
        html.append("</tbody></table>")
                .append("<div class=\"items-summary\">")
                .append("<b>Total colis:</b> ").append(order != null ? order.getItems().size() : 0).append(" article(s) &#160;&#183;&#160; ")
                .append(totalQty).append(" unit&#233;(s) &#160;&#183;&#160; ")
                .append("<b>Poids estim&#233;:</b> ~").append(totalQty * 0.5).append(" kg")
                .append("</div>");

        return this;
    }

    @Override
    public DocumentBuilder addTotals() {
        // Pour un bon de livraison, on n'affiche pas les montants financiers
        // mais plutôt un récapitulatif logistique
        html.append("<table class=\"logistics-grid\"><tr>")

                .append("<td class=\"logistics-left\">")
                .append("<div class=\"sub-title\">CONDITIONS DE LIVRAISON</div>")
                .append("<div class=\"conditions-list\">")
                .append("&#8226; V&#233;rifier l'int&#233;grit&#233; du colis &#224; r&#233;ception<br/>")
                .append("&#8226; Signaler tout dommage sous 24h<br/>")
                .append("&#8226; Conserver l'emballage d'origine<br/>")
                .append("&#8226; Retour gratuit sous 7 jours")
                .append("</div>")

                .append("<div class=\"sub-title\" style=\"margin-top:8pt\">CONTACT SUPPORT</div>")
                .append("<div class=\"support-info\">")
                .append("T&#233;l: ").append(esc(companyPhone)).append("<br/>")
                .append("Email: ").append(esc(companyEmail)).append("<br/>")
                .append("Horaires: Lun-Ven 8h-18h")
                .append("</div>")
                .append("</td>")

                .append("<td class=\"logistics-right\">")
                .append("<div class=\"sub-title\">SIGNATURES</div>")
                .append("<table class=\"signatures-table\">")
                .append("<tr><td class=\"sig-header\">Pr&#233;par&#233; par</td><td class=\"sig-header\">Re&#231;u par</td></tr>")
                .append("<tr><td class=\"sig-box\">&#160;</td><td class=\"sig-box\">&#160;</td></tr>")
                .append("<tr><td class=\"sig-label\">Nom et date</td><td class=\"sig-label\">Nom, date et cachet</td></tr>")
                .append("</table>")

                .append("<div class=\"qr-placeholder\">")
                .append("<div class=\"qr-box\">QR</div>")
                .append("<div class=\"qr-text\">Scannez pour suivre<br/>votre livraison</div>")
                .append("</div>")
                .append("</td>")

                .append("</tr></table>");

        return this;
    }

    @Override
    public DocumentBuilder addFooter() {
        html.append("<div class=\"footer-divider\"></div>")
                .append("<table class=\"footer-grid\"><tr>")

                .append("<td class=\"footer-col\">")
                .append("<div class=\"sub-title\">MENTIONS L&#201;GALES</div>")
                .append("<div class=\"footer-text\">")
                .append("Ce bon de livraison atteste de la remise des articles list&#233;s ci-dessus. " +
                        "Le destinataire est tenu de v&#233;rifier la conformit&#233; et l'&#233;tat des marchandises " +
                        "avant de signer. Toute r&#233;clamation doit &#234;tre formul&#233;e dans les 24h.")
                .append("</div>")
                .append("</td>")

                .append("<td class=\"footer-col footer-legal\">")
                .append("Document g&#233;n&#233;r&#233; &#233;lectroniquement<br/>")
                .append("Valide sans signature pr&#233;alable<br/>")
                .append("&#169; ").append(java.time.Year.now()).append(" ").append(esc(companyName))
                .append("</td>")

                .append("</tr></table>")

                .append("<div class=\"footer-banner\">")
                .append("Merci pour votre confiance &#8212; ").append(esc(companyWebsite))
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
            log.info("Bon de livraison PDF g&#233;n&#233;r&#233; ({} octets)", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur g&#233;n&#233;ration bon de livraison PDF", e);
            throw new RuntimeException("&#201;chec g&#233;n&#233;ration bon de livraison PDF", e);
        }
    }

    private String buildCss() {
        return
                "@page { size: A4; margin: 12mm 12mm 10mm 12mm; }" +
                        "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                        "body { font-family: Helvetica, Arial, sans-serif; font-size: 8pt; color: #1e2632; line-height: 1.35; }" +

                        // Header
                        ".doc-header { width: 100%; border-collapse: collapse; margin-bottom: 6pt; }" +
                        ".header-left { background: #2c5282; width: 60%; padding: 10pt 12pt; vertical-align: middle; }" +
                        ".header-right { background: #38a169; width: 40%; padding: 10pt 12pt; vertical-align: middle; }" +
                        ".company-name { font-size: 16pt; font-weight: bold; color: #ffffff; letter-spacing: 1px; margin-bottom: 2pt; }" +
                        ".company-sub { font-size: 7pt; color: #bee3f8; margin-bottom: 4pt; }" +
                        ".company-contacts { font-size: 7pt; color: #90cdf4; line-height: 1.5; }" +
                        ".logo-img { max-height: 36pt; margin-bottom: 5pt; display: block; }" +
                        ".doc-badge { font-size: 18pt; font-weight: bold; color: #ffffff; text-align: right; letter-spacing: 1px; margin-bottom: 6pt; }" +
                        ".meta-table { width: 100%; border-collapse: collapse; }" +
                        ".meta-table td { font-size: 7.5pt; padding: 1.5pt 0; color: #ffffff; }" +
                        ".meta-table td.lbl { color: #c6f6d5; padding-right: 8pt; }" +
                        ".meta-table td.val { text-align: right; font-weight: bold; }" +

                        // Info grid
                        ".info-grid { width: 100%; border-collapse: collapse; margin: 7pt 0; }" +
                        ".info-box { width: 50%; padding: 6pt 8pt; vertical-align: top; border-left: 2.5pt solid #2c5282; background: #f7fafc; }" +
                        ".info-box--green { border-left-color: #38a169; background: #f0fff4; }" +
                        ".info-box__title { font-size: 7pt; font-weight: bold; color: #2c5282; margin-bottom: 3pt; }" +
                        ".info-box--green .info-box__title { color: #276749; }" +
                        ".info-box__name { font-weight: bold; font-size: 8.5pt; margin-bottom: 2pt; }" +
                        ".info-box__body { font-size: 7.5pt; color: #3d4a58; line-height: 1.5; }" +

                        // Instructions
                        ".instructions-box { background: #fffbeb; border: 0.5pt solid #f6e05e; border-left: 2.5pt solid #d69e2e; padding: 5pt 8pt; margin: 6pt 0; }" +
                        ".instructions-title { font-size: 7pt; font-weight: bold; color: #975a16; margin-bottom: 2pt; }" +
                        ".instructions-body { font-size: 7.5pt; color: #744210; line-height: 1.4; }" +

                        // Section title
                        ".section-title { font-size: 8pt; font-weight: bold; color: #2c5282; margin: 6pt 0 3pt; border-bottom: 1pt solid #bee3f8; padding-bottom: 2pt; }" +

                        // Items table
                        ".items-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".items-table thead tr { background: #2c5282; color: #ffffff; }" +
                        ".items-table thead th { padding: 5pt 6pt; font-weight: bold; font-size: 7pt; }" +
                        ".col-num { width: 5%; text-align: center; }" +
                        ".col-desc { width: 45%; text-align: left; }" +
                        ".col-sku { width: 20%; text-align: center; }" +
                        ".col-qty { width: 15%; text-align: center; }" +
                        ".col-check { width: 15%; text-align: center; }" +
                        ".items-table tbody td { padding: 5pt 6pt; vertical-align: middle; border-bottom: 0.5pt solid #e2e8f0; }" +
                        ".row-alt { background: #f7fafc; }" +
                        ".sku { font-size: 6.5pt; color: #718096; font-family: monospace; }" +
                        ".check-box { font-size: 12pt; color: #cbd5e0; }" +
                        ".items-summary { text-align: right; font-size: 7pt; color: #4a5568; font-style: italic; margin: 4pt 0 8pt; padding: 3pt; background: #edf2f7; }" +

                        // Logistics grid
                        ".logistics-grid { width: 100%; border-collapse: collapse; margin-top: 8pt; }" +
                        ".logistics-left { width: 55%; vertical-align: top; padding-right: 10pt; }" +
                        ".logistics-right { width: 45%; vertical-align: top; }" +
                        ".sub-title { font-size: 7pt; font-weight: bold; color: #2c5282; margin-bottom: 3pt; text-transform: uppercase; }" +
                        ".conditions-list { font-size: 7.5pt; color: #4a5568; line-height: 1.6; background: #f7fafc; padding: 5pt 8pt; border-left: 2pt solid #38a169; }" +
                        ".support-info { font-size: 7.5pt; color: #4a5568; line-height: 1.5; background: #ebf8ff; padding: 5pt 8pt; border-left: 2pt solid #4299e1; }" +

                        // Signatures
                        ".signatures-table { width: 100%; border-collapse: collapse; margin-bottom: 6pt; }" +
                        ".sig-header { font-size: 7pt; font-weight: bold; color: #4a5568; padding: 2pt 0; text-align: center; }" +
                        ".sig-box { border: 0.5pt solid #cbd5e0; height: 32pt; background: #ffffff; }" +
                        ".sig-label { font-size: 6pt; color: #a0aec0; text-align: center; padding-top: 2pt; }" +

                        // QR placeholder
                        ".qr-placeholder { text-align: center; padding: 5pt; background: #f7fafc; border: 0.5pt dashed #cbd5e0; }" +
                        ".qr-box { width: 40pt; height: 40pt; background: #e2e8f0; margin: 0 auto 3pt; line-height: 40pt; font-size: 8pt; color: #718096; font-weight: bold; }" +
                        ".qr-text { font-size: 6pt; color: #718096; line-height: 1.3; }" +

                        // Footer
                        ".footer-divider { border-top: 0.5pt solid #bee3f8; margin: 8pt 0; }" +
                        ".footer-grid { width: 100%; border-collapse: collapse; }" +
                        ".footer-col { width: 70%; vertical-align: top; padding-right: 8pt; font-size: 7pt; }" +
                        ".footer-text { color: #4a5568; line-height: 1.5; }" +
                        ".footer-legal { width: 30%; color: #718096; line-height: 1.6; text-align: right; font-size: 6.5pt; }" +
                        ".footer-banner { background: #2c5282; color: #ffffff; text-align: center; font-size: 7.5pt; font-style: italic; padding: 5pt; margin-top: 8pt; }" +

                        // Utilities
                        ".center { text-align: center; }" +
                        ".right { text-align: right; }" +
                        ".bold { font-weight: bold; }" +
                        ".muted { color: #718096; }";
    }

    private String metaRow(String label, String value) {
        return "<tr><td class=\"lbl\">" + label + "</td><td class=\"val\">" + value + "</td></tr>";
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
            log.debug("Logo BL non trouv&#233;: {}", logoPath);
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
