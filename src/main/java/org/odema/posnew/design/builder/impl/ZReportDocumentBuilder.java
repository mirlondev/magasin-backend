package org.odema.posnew.design.builder.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.entity.ShiftReport;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;

/**
 * Builder pour les rapports Z (fin de journée) de caisse.
 * Format thermique 80mm - Lecture des compteurs avec remise à zéro.
 * Document officiel pour la clôture de caisse.
 */
@Slf4j
@Component
public class ZReportDocumentBuilder extends AbstractPdfDocumentBuilder {

    private static final DateTimeFormatter FMT_DT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FMT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String companyName        = "ODEMA POS";
    private String companyAddress     = "123 Rue Principale";
    private String companyPhone       = "+237 6XX XX XX XX";
    private String companyTaxId       = "TAX-123456";
    private String logoPath           = "static/logo.png";

    private ShiftReport shiftReport;
    private Map<String, Object> additionalData;
    private final StringBuilder html = new StringBuilder();

    public ZReportDocumentBuilder() { super(null); }

    public ZReportDocumentBuilder withConfig(String companyName, String companyAddress,
                                             String companyPhone, String companyTaxId) {
        if (companyName    != null) this.companyName    = companyName;
        if (companyAddress != null) this.companyAddress = companyAddress;
        if (companyPhone   != null) this.companyPhone   = companyPhone;
        if (companyTaxId   != null) this.companyTaxId   = companyTaxId;
        return this;
    }

    public ZReportDocumentBuilder withShiftReport(ShiftReport shiftReport) {
        this.shiftReport = shiftReport;
        return this;
    }

    public ZReportDocumentBuilder withAdditionalData(Map<String, Object> data) {
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

        // Bandeau Z Report - Fermeture officielle
        html.append("<div class=\"report-banner z-banner\">");
        html.append("<span class=\"report-icon\">&#9199;</span> ");
        html.append("RAPPORT Z");
        html.append(" <span class=\"report-icon\">&#9199;</span>");
        html.append("</div>");
        html.append("<div class=\"report-subtitle\">Cl&#244;ture de caisse &#8212; Remise &#224; z&#233;ro</div>");

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
        html.append("<div class=\"doc-type\">RAPPORT DE CL&#212;TURE (Z)</div>")
                .append("<table class=\"info-table\">");

        if (shiftReport != null) {
            String sessionId = shiftReport.getShiftReportId() != null
                    ? shiftReport.getShiftReportId().toString().substring(0, 8).toUpperCase()
                    : "&#8212;";

            html.append(infoRow("N&#176; Session", sessionId))
                    .append(infoRow("Caisse", shiftReport.getStore() != null
                            ? esc(shiftReport.getStore().getName()) : "&#8212;"))
                    .append(infoRow("Caissier", shiftReport.getCashier() != null
                            ? esc(shiftReport.getCashier().getUsername()) : "&#8212;"))
                    .append(infoRow("Date", shiftReport.getClosingTime() != null
                            ? shiftReport.getClosingTime().format(FMT_DATE) : "&#8212;"))
                    .append(infoRow("Ouverture", shiftReport.getOpeningTime() != null
                            ? shiftReport.getOpeningTime().format(FMT_DT) : "&#8212;"))
                    .append(infoRow("Fermeture", shiftReport.getClosingTime() != null
                            ? shiftReport.getClosingTime().format(FMT_DT) : "&#8212;"));
        } else {
            html.append(infoRow("N&#176; Session", "&#8212;"))
                    .append(infoRow("Date", java.time.LocalDateTime.now().format(FMT_DATE)))
                    .append(infoRow("G&#233;n&#233;r&#233; le", java.time.LocalDateTime.now().format(FMT_DT)));
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addItemsTable() {
        // Ventes détaillées
        html.append("<div class=\"section-title\">&#128176; D&#201;TAIL DES VENTES</div>")
                .append("<table class=\"detail-table\">");

        if (shiftReport != null) {
            // Ventes par catégorie
            html.append(detailRow("Ventes comptant", shiftReport.getCashSales(), true))
                    .append(detailRow("Ventes carte", shiftReport.getCardSales(), true))
                    .append(detailRow("Ventes Mobile Money", shiftReport.getMobileMoneySales(), true))
                    .append(detailRow("Ventes cr&#233;dit", shiftReport.getCreditSales(), true));

            html.append("<tr class=\"subtotal-row\"><td>Sous-total ventes</td><td>")
                    .append(fmtCur(shiftReport.getTotalSales())).append("</td></tr>");

            // Annulations et remboursements
            html.append("<tr><td colspan=\"2\" class=\"spacer\"></td></tr>");
            html.append(detailRow("Annulations", shiftReport.getTotalCancellations(), false))
                    .append(detailRow("Remboursements", shiftReport.getTotalRefunds(), false));
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");

        // Mouvements de caisse
        html.append("<div class=\"section-title\">&#128188; MOUVEMENTS DE CAISSE</div>")
                .append("<table class=\"detail-table\">");

        if (shiftReport != null) {
            html.append(detailRow("Fond de caisse initial", shiftReport.getOpeningBalance(), true))
                    .append(detailRow("Entr&#233;es de caisse", shiftReport.getTotalCashIn(), true))
                    .append(detailRow("Sorties de caisse", shiftReport.getTotalCashOut(), false))
                    .append("<tr class=\"subtotal-row\"><td>Flux net caisse</td><td>")
                    .append(fmtCur(calculateNetCashFlow())).append("</td></tr>");
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");

        // Statistiques
        html.append("<div class=\"section-title\">&#128202; STATISTIQUES</div>")
                .append("<table class=\"stats-table\">");

        if (shiftReport != null) {
            int totalTrans = shiftReport.getTotalTransactions() != null ? shiftReport.getTotalTransactions() : 0;
            BigDecimal totalSales = shiftReport.getTotalSales() != null ? shiftReport.getTotalSales() : BigDecimal.ZERO;
            BigDecimal avgTicket = totalTrans > 0
                    ? totalSales.divide(new BigDecimal(totalTrans), 0, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO;

            html.append(statRow("Nombre de transactions", String.valueOf(totalTrans)))
                    .append(statRow("Ticket moyen", fmtCur(avgTicket)))
                    .append(statRow("Articles vendus", String.valueOf(
                            additionalData != null && additionalData.get("totalItems") != null
                                    ? additionalData.get("totalItems") : "&#8212;")))
                    .append(statRow("Clients servis", String.valueOf(
                            additionalData != null && additionalData.get("totalCustomers") != null
                                    ? additionalData.get("totalCustomers") : "&#8212;")));
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");

        return this;
    }

    @Override
    public DocumentBuilder addTotals() {
        html.append("<div class=\"section-title\">&#128281; BILAN DE CL&#212;TURE</div>")
                .append("<table class=\"final-table\">");

        if (shiftReport != null) {
            // Total des ventes du jour
            BigDecimal totalSales = shiftReport.getTotalSales() != null ? shiftReport.getTotalSales() : BigDecimal.ZERO;
            html.append(finalRow("TOTAL VENTES JOURN&#201;E", totalSales, "total-sales"));

            // Solde théorique
            BigDecimal expectedBalance = shiftReport.getExpectedBalance() != null
                    ? shiftReport.getExpectedBalance() : BigDecimal.ZERO;
            html.append(finalRow("Solde th&#233;orique caisse", expectedBalance, "expected"));

            // Solde réel compté
            if (shiftReport.getActualBalance() != null) {
                html.append(finalRow("Solde r&#233;el compt&#233;", shiftReport.getActualBalance(), "actual"));

                // Écart
                BigDecimal difference = shiftReport.getDifference() != null
                        ? shiftReport.getDifference() : BigDecimal.ZERO;
                String diffClass = difference.compareTo(BigDecimal.ZERO) > 0 ? "surplus" :
                        (difference.compareTo(BigDecimal.ZERO) < 0 ? "deficit" : "balanced");
                String diffLabel = difference.compareTo(BigDecimal.ZERO) > 0 ? "Surplus (&#224; justifier)" :
                        (difference.compareTo(BigDecimal.ZERO) < 0 ? "D&#233;ficit (&#224; justifier)" : "&#201;quilibre parfait");
                html.append(finalRow(diffLabel, difference.abs(), diffClass));
            }

            // Fond de caisse pour demain
            html.append("<tr class=\"separator\"><td colspan=\"2\">&#160;</td></tr>");
            BigDecimal tomorrowFloat = shiftReport.getClosingBalance() != null
                    ? shiftReport.getClosingBalance() : BigDecimal.ZERO;
            html.append(finalRow("Fond de caisse report&#233;", tomorrowFloat, "tomorrow"));
        }

        html.append("</table>")
                .append("<div class=\"sep-dashed\">&#160;</div>");
        return this;
    }

    @Override
    public DocumentBuilder addFooter() {
        // Message de clôture
        html.append("<div class=\"closure-notice\">")
                .append("<b>&#10004; CL&#212;TURE VALID&#201;E</b><br/>")
                .append("Les compteurs ont &#233;t&#233; remis &#224; z&#233;ro.<br/>")
                .append("Cette session est d&#233;sormais archiv&#233;e.")
                .append("</div>");

        // Signatures obligatoires
        html.append("<div class=\"signatures-title\">SIGNATURES DE CL&#212;TURE</div>")
                .append("<div class=\"signatures\">")
                .append("<div class=\"sig-col\">")
                .append("<div class=\"sig-label\">Caissier</div>")
                .append("<div class=\"sig-box\">&#160;</div>")
                .append("<div class=\"sig-date\">Date: ____/____/________</div>")
                .append("</div>")
                .append("<div class=\"sig-col\">")
                .append("<div class=\"sig-label\">Responsable / Manager</div>")
                .append("<div class=\"sig-box\">&#160;</div>")
                .append("<div class=\"sig-date\">Date: ____/____/________</div>")
                .append("</div>")
                .append("</div>");

        // Mentions légales
        html.append("<div class=\"legal-text\">")
                .append("<b>Document officiel de cl&#244;ture de caisse</b><br/>")
                .append("Conform&#233;ment &#224; la r&#233;glementation fiscale en vigueur.<br/>")
                .append("&#169; ").append(java.time.Year.now()).append(" ").append(esc(companyName))
                .append(" &#8212; NIF: ").append(esc(companyTaxId))
                .append("</div>");

        // Codes barres et référence
        if (shiftReport != null && shiftReport.getShiftReportId() != null) {
            html.append("<div class=\"barcode-line\">* Z-")
                    .append(shiftReport.getShiftReportId().toString().substring(0, 8).toUpperCase())
                    .append(" *</div>");
        }

        html.append("<div class=\"cut-line\">- - - - - - &#9988; - - - - - -</div>")
                .append("<div class=\"archive-notice\">CONSERVER POUR ARCHIVES &#8212; COPIE CLIENT</div>")
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
            log.info("Rapport Z PDF g&#233;n&#233;r&#233; ({} octets)", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur g&#233;n&#233;ration rapport Z PDF: {}", e.getMessage());
            throw new RuntimeException("&#201;chec g&#233;n&#233;ration rapport Z PDF", e);
        }
    }

    private BigDecimal calculateNetCashFlow() {
        if (shiftReport == null) return BigDecimal.ZERO;
        BigDecimal opening = shiftReport.getOpeningBalance() != null ? shiftReport.getOpeningBalance() : BigDecimal.ZERO;
        BigDecimal cashIn = shiftReport.getTotalCashIn() != null ? shiftReport.getTotalCashIn() : BigDecimal.ZERO;
        BigDecimal cashOut = shiftReport.getTotalCashOut() != null ? shiftReport.getTotalCashOut() : BigDecimal.ZERO;
        return opening.add(cashIn).subtract(cashOut);
    }

    private String buildCss() {
        return
                "@page { size: 80mm auto; margin: 4mm 3mm; }" +
                        "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                        "body { font-family: 'Courier New', Courier, monospace; font-size: 8pt; color: #1a1a1a; line-height: 1.3; width: 74mm; }" +

                        // Bannière Z
                        ".report-banner { text-align: center; font-size: 14pt; font-weight: bold; padding: 6pt; margin-bottom: 2pt; letter-spacing: 2pt; color: #ffffff; }" +
                        ".z-banner { background: #2f855a; }" +
                        ".report-icon { font-size: 16pt; }" +
                        ".report-subtitle { text-align: center; font-size: 7pt; color: #718096; margin-bottom: 4pt; font-style: italic; }" +

                        // Header
                        ".header { text-align: center; padding-bottom: 4pt; }" +
                        ".logo-wrap img { max-width: 48pt; max-height: 36pt; margin-bottom: 3pt; }" +
                        ".company-name { font-size: 12pt; font-weight: bold; letter-spacing: 1pt; margin-bottom: 2pt; }" +
                        ".company-info { font-size: 7pt; color: #444; line-height: 1.4; }" +

                        // Séparateurs
                        ".sep-dashed { border-top: 1pt dashed #999; margin: 4pt 0; font-size: 1pt; color: transparent; }" +

                        // Type document
                        ".doc-type { text-align: center; font-size: 10pt; font-weight: bold; letter-spacing: 1pt; padding: 3pt 0; color: #2f855a; }" +

                        // Sections
                        ".section-title { font-size: 7pt; font-weight: bold; color: #2d3748; text-transform: uppercase; letter-spacing: 0.5pt; margin: 4pt 0 2pt; border-bottom: 0.5pt solid #9ae6b4; padding-bottom: 1pt; }" +

                        // Table infos
                        ".info-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".info-table td { padding: 1pt 2pt; vertical-align: top; }" +
                        ".info-table td:first-child { font-weight: bold; width: 42%; color: #4a5568; white-space: nowrap; }" +

                        // Tables détail
                        ".detail-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".detail-table td { padding: 2pt 2pt; border-bottom: 0.5pt dotted #e2e8f0; }" +
                        ".detail-table td:first-child { width: 60%; }" +
                        ".detail-table td:last-child { width: 40%; text-align: right; font-weight: bold; }" +
                        ".subtotal-row { background: #f0fff4; font-weight: bold; }" +
                        ".subtotal-row td { padding: 3pt 2pt; border-top: 1pt solid #9ae6b4; color: #2f855a; }" +
                        ".spacer { height: 4pt; border: none; }" +
                        ".positive { color: #38a169; }" +
                        ".negative { color: #e53e3e; }" +

                        // Stats table
                        ".stats-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".stats-table td { padding: 2pt 2pt; }" +
                        ".stats-table td:first-child { width: 60%; color: #4a5568; }" +
                        ".stats-table td:last-child { width: 40%; text-align: right; font-weight: bold; color: #2b6cb0; }" +

                        // Final table
                        ".final-table { width: 100%; border-collapse: collapse; font-size: 8pt; margin: 2pt 0; }" +
                        ".final-table td { padding: 3pt 2pt; }" +
                        ".final-table td:first-child { width: 55%; font-weight: bold; }" +
                        ".final-table td:last-child { width: 45%; text-align: right; font-weight: bold; }" +
                        ".separator td { border-top: 1pt solid #2f855a; padding: 2pt 0; font-size: 2pt; }" +
                        ".total-sales { font-size: 11pt; color: #2f855a; background: #f0fff4; }" +
                        ".expected { font-size: 9pt; color: #2b6cb0; background: #ebf8ff; }" +
                        ".actual { font-size: 9pt; color: #744210; background: #fffbeb; }" +
                        ".surplus { font-size: 9pt; color: #38a169; background: #c6f6d5; }" +
                        ".deficit { font-size: 9pt; color: #c53030; background: #fed7d7; }" +
                        ".balanced { font-size: 9pt; color: #2f855a; background: #9ae6b4; }" +
                        ".tomorrow { font-size: 10pt; color: #553c9a; background: #faf5ff; border-top: 1pt solid #d6bcfa; }" +

                        // Clôture notice
                        ".closure-notice { background: #f0fff4; border: 1pt solid #68d391; color: #276749; font-size: 7pt; padding: 5pt; margin: 4pt 0; text-align: center; line-height: 1.5; }" +

                        // Signatures
                        ".signatures-title { text-align: center; font-size: 7pt; font-weight: bold; color: #4a5568; margin: 6pt 0 3pt; text-transform: uppercase; }" +
                        ".signatures { display: table; width: 100%; margin-top: 4pt; }" +
                        ".sig-col { display: table-cell; width: 50%; text-align: center; padding: 0 3pt; }" +
                        ".sig-label { font-size: 6.5pt; color: #4a5568; margin-bottom: 2pt; font-weight: bold; }" +
                        ".sig-box { border: 0.5pt solid #a0aec0; height: 24pt; background: #ffffff; }" +
                        ".sig-date { font-size: 6pt; color: #718096; margin-top: 2pt; }" +

                        // Footer
                        ".legal-text { text-align: center; font-size: 6.5pt; color: #4a5568; line-height: 1.5; padding: 4pt 0; border-top: 0.5pt solid #e2e8f0; margin-top: 4pt; }" +
                        ".barcode-line { text-align: center; font-size: 9pt; letter-spacing: 1pt; padding: 4pt 0; color: #2f855a; font-weight: bold; }" +
                        ".cut-line { text-align: center; font-size: 8pt; color: #cbd5e0; margin-top: 4pt; letter-spacing: 1pt; }" +
                        ".archive-notice { text-align: center; font-size: 6pt; color: #a0aec0; margin-top: 4pt; font-weight: bold; letter-spacing: 0.5pt; }" +

                        // Utilitaires
                        ".center { text-align: center; }" +
                        ".right { text-align: right; }" +
                        ".bold { font-weight: bold; }";
    }

    private String infoRow(String label, String value) {
        return "<tr><td>" + label + ":</td><td>" + value + "</td></tr>";
    }

    private String detailRow(String label, BigDecimal amount, boolean isPositive) {
        String cssClass = isPositive ? "positive" : "negative";
        BigDecimal value = amount != null ? amount : BigDecimal.ZERO;
        return "<tr><td>" + esc(label) + "</td><td class=\"" + cssClass + "\">" + fmtCur(value) + "</td></tr>";
    }

    private String statRow(String label, String value) {
        return "<tr><td>" + esc(label) + "</td><td>" + value + "</td></tr>";
    }

    private String finalRow(String label, BigDecimal amount, String cssClass) {
        return "<tr class=\"" + cssClass + "\"><td>" + esc(label) + "</td><td>" + fmtCur(amount) + "</td></tr>";
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
            log.debug("Logo rapport Z non trouv&#233;: {}", logoPath);
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
