package org.odema.posnew.design.builder.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.domain.model.ShiftReport;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

/**
 * Builder pour les rapports X (intermédiaires) de caisse.
 * Format thermique 80mm - Lecture des compteurs sans remise à zéro.
 */
@Slf4j
@Component
public class XReportDocumentBuilder extends AbstractPdfDocumentBuilder {

    private static final DateTimeFormatter FMT_DT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FMT_TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private String companyName        = "ODEMA POS";
    private String companyAddress     = "123 Rue Principale";
    private String companyPhone       = "+237 6XX XX XX XX";
    private String logoPath           = "static/logo.png";

    private ShiftReport shiftReport;
    private Map<String, Object> additionalData;
    private final StringBuilder html = new StringBuilder();

    public XReportDocumentBuilder() { super(null); }

    public XReportDocumentBuilder withConfig(String companyName, String companyAddress,
                                             String companyPhone) {
        if (companyName    != null) this.companyName    = companyName;
        if (companyAddress != null) this.companyAddress = companyAddress;
        if (companyPhone   != null) this.companyPhone   = companyPhone;
        return this;
    }

    public XReportDocumentBuilder withShiftReport(ShiftReport shiftReport) {
        this.shiftReport = shiftReport;
        return this;
    }

    public XReportDocumentBuilder withAdditionalData(Map<String, Object> data) {
        this.additionalData = data;
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

        // Bandeau X Report
        html.append("<div class=\"report-banner x-banner\">");
        html.append("<span class=\"report-icon\">&#119974;</span> ");
        html.append("RAPPORT X");
        html.append(" <span class=\"report-icon\">&#119974;</span>");
        html.append("</div>");
        html.append("<div class=\"report-subtitle\">Lecture interm&#233;diaire</div>");

        html.append("<div class=\"header\">")
                .append(logoTag.isEmpty() ? "" : "<div class=\"logo-wrap\">" + logoTag + "</div>")
                .append("<div class=\"company-name\">").append(esc(companyName)).append("</div>");
        if (!companyAddress.isBlank())
            html.append("<div class=\"company-info\">").append(esc(companyAddress)).append("</div>");
        if (!companyPhone.isBlank())
            html.append("<div class=\"company-info\">T&#233;l: ").append(esc(companyPhone)).append("</div>");
        html.append("</div>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addMainInfo() {
        html.append("<div class=\"doc-type\">RAPPORT DE CAISSE (X)</div>")
                .append("<table class=\"info-table\">");

        if (shiftReport != null) {
            // ID Session (8 premiers caractères)
            String sessionId = shiftReport.getShiftReportId() != null
                    ? shiftReport.getShiftReportId().toString().substring(0, 8).toUpperCase()
                    : "&#8212;";

            // Numéro de session (nouveau champ)
            String shiftNumber = shiftReport.getShiftNumber() != null
                    ? shiftReport.getShiftNumber()
                    : sessionId;

            // Caisse
            String registerName = "&#8212;";
            if (shiftReport.getCashRegister() != null) {
                registerName = shiftReport.getCashRegister().getName();
                if (shiftReport.getCashRegister().getRegisterNumber() != null) {
                    registerName += " (" + shiftReport.getCashRegister().getRegisterNumber() + ")";
                }
            }

            // Caissier
            String cashierName = "&#8212;";
            if (shiftReport.getCashier() != null) {
                cashierName = shiftReport.getCashier().getUsername();
                if (shiftReport.getCashier().getFullName() != null) {
                    cashierName += " - " + shiftReport.getCashier().getFullName();
                }
            }

            // Store
            String storeName = "&#8212;";
            if (shiftReport.getStore() != null) {
                storeName = shiftReport.getStore().getName();
            }

            html.append(infoRow("Session", shiftNumber))
                    .append(infoRow("Caisse", registerName))
                    .append(infoRow("Magasin", storeName))
                    .append(infoRow("Caissier", cashierName))
                    .append(infoRow("Ouverture", shiftReport.getOpeningTime() != null
                            ? shiftReport.getOpeningTime().format(FMT_DT) : "&#8212;"))
                    .append(infoRow("Statut", shiftReport.getStatus() != null
                            ? shiftReport.getStatus().getLabel() : "&#8212;"))
                    .append(infoRow("Rapport g&#233;n&#233;r&#233;", java.time.LocalDateTime.now().format(FMT_DT)));
        } else {
            html.append(infoRow("Session", "&#8212;"))
                    .append(infoRow("Caisse", "&#8212;"))
                    .append(infoRow("Caissier", "&#8212;"))
                    .append(infoRow("Rapport g&#233;n&#233;r&#233;", java.time.LocalDateTime.now().format(FMT_DT)));
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addItemsTable() {
        // Section des ventes par méthode de paiement
        html.append("<div class=\"section-title\">VENTES PAR MODE DE PAIEMENT</div>")
                .append("<table class=\"payment-table\">");

        if (shiftReport != null) {
            // Espèces
            html.append(paymentRow("Esp&#232;ces",
                    shiftReport.getCashSales(),
                    shiftReport.getCashSalesCount()));

            // Carte bancaire
            html.append(paymentRow("Carte bancaire",
                    shiftReport.getCardSales(),
                    shiftReport.getCardSalesCount()));

            // Mobile Money
            html.append(paymentRow("Mobile Money",
                    shiftReport.getMobileMoneySales(),
                    shiftReport.getMobileMoneySalesCount()));

            // Crédit
            html.append(paymentRow("Cr&#233;dit",
                    shiftReport.getCreditSales(),
                    shiftReport.getCreditSalesCount()));
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");

        // Section des mouvements de caisse
        html.append("<div class=\"section-title\">MOUVEMENTS DE CAISSE</div>")
                .append("<table class=\"movement-table\">");

        if (shiftReport != null) {
            html.append(movementRow("Fond de caisse initial", shiftReport.getOpeningBalance(), true))
                    .append(movementRow("Entr&#233;es de caisse", shiftReport.getTotalCashIn(), true))
                    .append(movementRow("Sorties de caisse", shiftReport.getTotalCashOut(), false))
                    .append(movementRow("Remboursements", shiftReport.getTotalRefunds(), false))
                    .append(movementRow("Annulations", shiftReport.getTotalCancellations(), false));
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");

        return this;
    }

    @Override
    public DocumentBuilder addTotals() {
        html.append("<div class=\"section-title\">R&#201;CAPITULATIF</div>")
                .append("<table class=\"totals-table\">");

        if (shiftReport != null) {
            // Total des ventes
            html.append(summaryRow("TOTAL VENTES", shiftReport.getTotalSales(), "total-sales"));

            // Ventes Nettes (nouveau)
            html.append(summaryRow("Ventes Nettes", shiftReport.getNetSales(), "net-sales"));

            // Nombre de transactions
            html.append(summaryRow("Nombre de transactions", String.valueOf(shiftReport.getTotalTransactions()), "count-row"));

            // Ticket moyen
            BigDecimal avgTicket = shiftReport.getTotalTransactions() > 0
                    ? shiftReport.getTotalSales().divide(new BigDecimal(shiftReport.getTotalTransactions()), 0, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;
            html.append(summaryRow("Ticket moyen", avgTicket, "avg-row"));

            html.append("<tr class=\"total-sep\"><td colspan=\"2\">&#160;</td></tr>");

            // Solde théorique caisse
            html.append(summaryRow("Solde th&#233;orique caisse", shiftReport.getExpectedBalance(), "expected-balance"));

            // Solde réel (si saisi)
            if (shiftReport.getActualBalance() != null && shiftReport.getActualBalance().compareTo(BigDecimal.ZERO) > 0) {
                html.append(summaryRow("Solde r&#233;el caisse", shiftReport.getActualBalance(), "actual-balance"));

                // Écart
                BigDecimal difference = shiftReport.getDifference();
                String diffClass = difference.compareTo(BigDecimal.ZERO) >= 0 ? "positive-diff" : "negative-diff";
                html.append(summaryRow("&#201;cart", difference, diffClass));
            }
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addFooter() {
        html.append("<div class=\"warning-box\">")
                .append("<b>&#8505; INFORMATION:</b> Ce rapport X est une lecture interm&#233;diaire " +
                        "des compteurs de caisse. Les totaux continueront de s'incr&#233;menter " +
                        "jusqu'&#224; la g&#233;n&#233;ration du rapport Z de fin de journ&#233;e.")
                .append("</div>");

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
                .append("Document g&#233;n&#233;r&#233; automatiquement &#8212; ")
                .append(java.time.LocalDateTime.now().format(FMT_DT))
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
            log.info("Rapport X PDF g&#233;n&#233;r&#233; ({} octets)", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur g&#233;n&#233;ration rapport X PDF: {}", e.getMessage(), e);
            throw new RuntimeException("&#201;chec g&#233;n&#233;ration rapport X PDF", e);
        }
    }

    private String buildCss() {
        return
                "@page { size: 80mm auto; margin: 4mm 3mm; }" +
                        "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                        "body { font-family: 'Courier New', Courier, monospace; font-size: 8pt; color: #1a1a1a; line-height: 1.3; width: 74mm; }" +

                        // Bannière X
                        ".report-banner { text-align: center; font-size: 12pt; font-weight: bold; padding: 5pt; margin-bottom: 2pt; letter-spacing: 2pt; color: #ffffff; }" +
                        ".x-banner { background: #3182ce; }" +
                        ".report-icon { font-size: 14pt; }" +
                        ".report-subtitle { text-align: center; font-size: 7pt; color: #718096; margin-bottom: 4pt; font-style: italic; }" +

                        // Header
                        ".header { text-align: center; padding-bottom: 4pt; }" +
                        ".logo-wrap img { max-width: 48pt; max-height: 36pt; margin-bottom: 3pt; }" +
                        ".company-name { font-size: 12pt; font-weight: bold; letter-spacing: 1pt; margin-bottom: 2pt; }" +
                        ".company-info { font-size: 7pt; color: #444; line-height: 1.4; }" +

                        // Séparateurs
                        ".sep-dashed { border-top: 1pt dashed #999; margin: 4pt 0; font-size: 1pt; color: transparent; }" +

                        // Type document
                        ".doc-type { text-align: center; font-size: 9pt; font-weight: bold; letter-spacing: 1pt; padding: 3pt 0; color: #3182ce; }" +

                        // Sections
                        ".section-title { font-size: 7pt; font-weight: bold; color: #2d3748; text-transform: uppercase; letter-spacing: 0.5pt; margin: 4pt 0 2pt; border-bottom: 0.5pt solid #e2e8f0; padding-bottom: 1pt; }" +

                        // Table infos
                        ".info-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".info-table td { padding: 1pt 2pt; vertical-align: top; }" +
                        ".info-table td:first-child { font-weight: bold; width: 40%; color: #4a5568; white-space: nowrap; }" +

                        // Table paiements
                        ".payment-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".payment-table td { padding: 2pt 2pt; border-bottom: 0.5pt dotted #e2e8f0; }" +
                        ".payment-table td:first-child { width: 45%; }" +
                        ".payment-table td:nth-child(2) { width: 35%; text-align: right; font-weight: bold; }" +
                        ".payment-table td:nth-child(3) { width: 20%; text-align: center; color: #718096; font-size: 6.5pt; }" +

                        // Table mouvements
                        ".movement-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".movement-table td { padding: 2pt 2pt; border-bottom: 0.5pt dotted #e2e8f0; }" +
                        ".movement-table td:first-child { width: 60%; }" +
                        ".movement-table td:last-child { width: 40%; text-align: right; font-weight: bold; }" +
                        ".positive { color: #38a169; }" +
                        ".negative { color: #e53e3e; }" +

                        // Table totaux
                        ".totals-table { width: 100%; border-collapse: collapse; font-size: 8pt; margin: 2pt 0; }" +
                        ".totals-table td { padding: 2pt 2pt; }" +
                        ".totals-table td:first-child { width: 55%; }" +
                        ".totals-table td:last-child { width: 45%; text-align: right; font-weight: bold; }" +
                        ".total-sep td { border-top: 1pt solid #3182ce; padding: 2pt 0 0; font-size: 2pt; color: transparent; }" +
                        ".total-sales { font-size: 10pt; color: #2b6cb0; }" +
                        ".net-sales { font-size: 9pt; color: #285e61; background: #e6fffa; }" +
                        ".count-row { font-size: 7.5pt; color: #4a5568; }" +
                        ".avg-row { font-size: 8pt; color: #744210; background: #fffbeb; }" +
                        ".expected-balance { font-size: 9pt; color: #2f855a; background: #f0fff4; }" +
                        ".actual-balance { font-size: 9pt; color: #2b6cb0; background: #ebf8ff; }" +
                        ".positive-diff { font-size: 9pt; color: #38a169; background: #c6f6d5; }" +
                        ".negative-diff { font-size: 9pt; color: #c53030; background: #fed7d7; }" +

                        // Warning box
                        ".warning-box { background: #ebf8ff; border: 0.5pt solid #90cdf4; color: #2b6cb0; font-size: 6.5pt; padding: 4pt; margin: 4pt 0; line-height: 1.4; }" +

                        // Signatures
                        ".signatures { display: table; width: 100%; margin-top: 6pt; }" +
                        ".sig-col { display: table-cell; width: 50%; text-align: center; padding: 0 4pt; }" +
                        ".sig-label { font-size: 7pt; color: #4a5568; margin-bottom: 2pt; }" +
                        ".sig-box { border-top: 0.5pt solid #a0aec0; height: 18pt; }" +

                        // Footer
                        ".legal-text { text-align: center; font-size: 6.5pt; color: #a0aec0; line-height: 1.5; padding: 4pt 0; }" +
                        ".cut-line { text-align: center; font-size: 8pt; color: #cbd5e0; margin-top: 6pt; letter-spacing: 1pt; }" +

                        // Utilitaires
                        ".center { text-align: center; }" +
                        ".right { text-align: right; }" +
                        ".bold { font-weight: bold; }";
    }

    private String infoRow(String label, String value) {
        return "<tr><td>" + label + ":</td><td>" + value + "</td></tr>";
    }

    private String paymentRow(String method, BigDecimal amount, int count) {
        return "<tr><td>" + esc(method) + "</td><td>" + fmtCur(amount) + "</td><td>(" + count + ")</td></tr>";
    }

    private String movementRow(String label, BigDecimal amount, boolean isPositive) {
        String cssClass = isPositive ? "positive" : "negative";
        String prefix = isPositive ? "+" : "-";
        BigDecimal value = amount != null ? amount : BigDecimal.ZERO;
        return "<tr><td>" + esc(label) + "</td><td class=\"" + cssClass + "\">" + prefix + fmtCur(value) + "</td></tr>";
    }

    private String summaryRow(String label, BigDecimal amount, String cssClass) {
        return "<tr class=\"" + cssClass + "\"><td>" + esc(label) + "</td><td>" + fmtCur(amount) + "</td></tr>";
    }

    private String summaryRow(String label, String value, String cssClass) {
        return "<tr class=\"" + cssClass + "\"><td>" + esc(label) + "</td><td>" + value + "</td></tr>";
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
            log.debug("Logo rapport X non trouv&#233;: {}", logoPath);
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