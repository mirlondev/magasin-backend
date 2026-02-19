package org.odema.posnew.design.builder.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.entity.Receipt;
import org.odema.posnew.entity.enums.ReceiptType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Ticket événements de caisse (ouverture/fermeture, entrée/sortie d'argent…)
 * Format thermique 80mm — HTML/CSS via openhtmltopdf.
 *
 * IMPORTANT — Règles XHTML strictes (openhtmltopdf parse du XML, pas du HTML5) :
 *  1. Toutes les données dynamiques passent par esc() pour encoder & < > "
 *  2. Pas d'entités HTML nommées (&nbsp; &mdash; etc.) → utiliser les codes numériques (&#160; &#8212;)
 *  3. DOCTYPE XHTML 1.0 Strict obligatoire
 *  4. Toutes les balises self-closing doivent avoir le slash : <br/> <img/> <meta/>
 */
@Slf4j
public class ShiftReceiptDocumentBuilder extends AbstractPdfDocumentBuilder {

    private static final DateTimeFormatter FMT_DT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Config société (valeurs par défaut)
    private String companyName        = "ODEMA POS";
    private String companyAddress     = "123 Rue Principale, Pointe-Noire";
    private String companyPhone       = "+237 6XX XX XX XX";
    private String companyTaxId       = "TAX-123456789";
    private String companyLogoPath    = "static/logo.png";

    private final Receipt receipt;
    private final StringBuilder html = new StringBuilder();

    public ShiftReceiptDocumentBuilder(Receipt receipt) {
        super(null);
        this.receipt = receipt;
    }

    public ShiftReceiptDocumentBuilder withConfig(
            String companyName, String companyAddress,
            String companyPhone, String companyTaxId) {
        if (companyName    != null) this.companyName    = companyName;
        if (companyAddress != null) this.companyAddress = companyAddress;
        if (companyPhone   != null) this.companyPhone   = companyPhone;
        if (companyTaxId   != null) this.companyTaxId   = companyTaxId;
        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // INITIALIZE
    // DOCTYPE XHTML 1.0 Strict requis par openhtmltopdf pour parser correctement
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public DocumentBuilder initialize() {
        this.outputStream = new ByteArrayOutputStream();
        html.setLength(0);
        html.append("<?xml version='1.0' encoding='UTF-8'?>")
                .append("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' ")
                .append("'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>")
                .append("<html xmlns='http://www.w3.org/1999/xhtml'>")
                .append("<head>")
                .append("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'/>")
                .append("<style>").append(buildCss()).append("</style>")
                .append("</head><body>");
        log.debug("ShiftReceiptDocumentBuilder HTML initialisé");
        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HEADER — nom société + coordonnées centrés
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public DocumentBuilder addHeader() {
        String logoTag = buildLogoTag();

        html.append("<div class='header'>")
                .append(logoTag.isEmpty() ? "" : "<div class='logo-wrap'>" + logoTag + "</div>")
                .append("<div class='company-name'>").append(esc(companyName)).append("</div>")
                .append("<div class='company-info'>").append(esc(companyAddress)).append("</div>")
                .append("<div class='company-info'>T&#233;l: ").append(esc(companyPhone)).append("</div>")
                .append("<div class='company-info'>NIF: ").append(esc(companyTaxId)).append("</div>")
                .append("</div>")
                .append("<div class='sep-dashed'>&#160;</div>");

        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // MAIN INFO — type de document + métadonnées session
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public DocumentBuilder addMainInfo() {
        // Badge type document avec couleur selon opération
        String badgeCss = getBadgeCss(receipt.getReceiptType());
        String typeLabel = formatTypeLabel(receipt.getReceiptType());

        html.append("<div class='doc-type ").append(badgeCss).append("'>").append(typeLabel).append("</div>");

        html.append("<table class='info-table'>")
                .append(infoRow("N&#176; Ticket",   esc(receipt.getReceiptNumber())))
                .append(infoRow("Date",        receipt.getReceiptDate().format(FMT_DT)))
                .append(infoRow("Caissier",    esc(receipt.getCashier().getUsername())))
                .append(infoRow("Point de vente", esc(receipt.getStore().getName())));

        if (receipt.getShiftReport() != null) {
            String sessionId = receipt.getShiftReport().getShiftReportId()
                    .toString().substring(0, 8).toUpperCase();
            html.append(infoRow("Session", sessionId));
        }

        html.append("</table>")
                .append("<div class='sep-dashed'>&#160;</div>");

        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ITEMS TABLE — notes / motif de l'opération (pas d'articles ici)
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public DocumentBuilder addItemsTable() {
        if (receipt.getNotes() != null && !receipt.getNotes().isBlank()) {
            html.append("<div class='notes-label'>Motif / Notes</div>")
                    .append("<div class='notes-box'>").append(esc(receipt.getNotes())).append("</div>")
                    .append("<div class='sep-dashed'>&#160;</div>");
        }
        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // TOTALS — montant principal selon type d'opération
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public DocumentBuilder addTotals() {
        if (receipt.getTotalAmount() == null
                || receipt.getTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
            return this;
        }

        String amountLabel = getAmountLabel(receipt.getReceiptType());
        String amountCss   = getAmountCss(receipt.getReceiptType());

        html.append("<table class='totals-table'>")
                .append("<tr class='total-main'>")
                .append("<td>").append(amountLabel).append("</td>")
                .append("<td class='right ").append(amountCss).append("'>")
                .append(fmtCur(receipt.getTotalAmount())).append("</td>")
                .append("</tr>")
                .append("</table>")
                .append("<div class='sep-dashed'>&#160;</div>");

        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // FOOTER — mentions légales + ligne de coupe
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public DocumentBuilder addFooter() {
        html.append("<div class='footer-legal'>")
                .append(esc(companyName)).append(" &#8212; Document de caisse<br/>")
                .append("Ce document est g&#233;n&#233;r&#233; automatiquement.")
                .append("</div>")
                .append("<div class='cut-line'>- - - - - - - - - - &#9986; - - - - - - - - - -</div>")
                .append("</body></html>");

        return this;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // BUILD — HTML → PDF
    // ════════════════════════════════════════════════════════════════════════════
    @Override
    public byte[] build() {
        try {
            String finalHtml = html.toString();
            ByteArrayOutputStream baos = (ByteArrayOutputStream) this.outputStream;

            // Log debug pour voir le HTML généré en cas d'erreur
            log.debug("HTML généré (500 premiers chars): {}", finalHtml.substring(0, Math.min(500, finalHtml.length())));

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(finalHtml, null);
            builder.toStream(baos);
            builder.run();

            log.info("Ticket caisse PDF généré ({} octets)", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur génération ticket caisse PDF", e);
            throw new RuntimeException("Échec génération ticket caisse PDF", e);
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CSS — concaténé en String (pas de text block) pour compatibilité
    // ════════════════════════════════════════════════════════════════════════════
    private String buildCss() {
        return
                "@page { size: 80mm auto; margin: 4mm 3mm; }" +
                        "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                        "body { font-family: 'Courier New', Courier, monospace; font-size: 8pt; color: #1a1a1a; line-height: 1.3; width: 74mm; }" +

                        /* ── En-tête ──────────────────────────────────────────────── */
                        ".header { text-align: center; padding-bottom: 4pt; }" +
                        ".logo-wrap img { max-width: 48pt; max-height: 36pt; margin-bottom: 3pt; }" +
                        ".company-name { font-size: 12pt; font-weight: bold; letter-spacing: 1pt; margin-bottom: 2pt; }" +
                        ".company-info { font-size: 7pt; color: #444; line-height: 1.4; }" +

                        /* ── Séparateurs ──────────────────────────────────────────── */
                        ".sep-dashed { border-top: 1pt dashed #999; margin: 4pt 0; font-size: 1pt; }" +

                        /* ── Badge type document ──────────────────────────────────── */
                        ".doc-type { text-align: center; font-size: 9pt; font-weight: bold; letter-spacing: 0.5pt; padding: 4pt 4pt; margin: 3pt 0; border-radius: 2pt; }" +

                        /* Variantes de couleur par type */
                        ".badge-opening  { background: #e8f5e9; color: #1b5e20; border: 1pt solid #a5d6a7; }" +
                        ".badge-closing  { background: #fff8e1; color: #e65100; border: 1pt solid #ffe082; }" +
                        ".badge-cashin   { background: #e3f2fd; color: #0d47a1; border: 1pt solid #90caf9; }" +
                        ".badge-cashout  { background: #fce4ec; color: #880e4f; border: 1pt solid #f48fb1; }" +
                        ".badge-refund   { background: #f3e5f5; color: #4a148c; border: 1pt solid #ce93d8; }" +
                        ".badge-cancel   { background: #ffebee; color: #b71c1c; border: 1pt solid #ef9a9a; }" +
                        ".badge-default  { background: #f5f5f5; color: #212121; border: 1pt solid #bdbdbd; }" +

                        /* ── Table infos ──────────────────────────────────────────── */
                        ".info-table { width: 100%; border-collapse: collapse; font-size: 7.5pt; }" +
                        ".info-table td { padding: 1.5pt 2pt; vertical-align: top; }" +
                        ".info-table td:first-child { font-weight: bold; width: 36%; color: #333; white-space: nowrap; }" +

                        /* ── Notes / motif ────────────────────────────────────────── */
                        ".notes-label { font-size: 7pt; font-weight: bold; color: #555; text-transform: uppercase; letter-spacing: 0.5pt; margin-bottom: 2pt; }" +
                        ".notes-box { font-size: 7.5pt; color: #333; background: #f9f9f9; border-left: 2pt solid #bbb; padding: 3pt 5pt; margin-bottom: 4pt; line-height: 1.4; }" +

                        /* ── Table totaux ─────────────────────────────────────────── */
                        ".totals-table { width: 100%; border-collapse: collapse; margin: 2pt 0; }" +
                        ".total-main td { font-size: 11pt; font-weight: bold; padding: 4pt 2pt; }" +
                        ".totals-table td:last-child { text-align: right; }" +

                        /* Couleurs montants */
                        ".amount-green  { color: #1a7a3a; }" +
                        ".amount-red    { color: #b42020; }" +
                        ".amount-orange { color: #b45a00; }" +
                        ".amount-blue   { color: #0d47a1; }" +

                        /* ── Footer ───────────────────────────────────────────────── */
                        ".footer-legal { text-align: center; font-size: 6.5pt; color: #666; line-height: 1.5; padding: 3pt 0; }" +
                        ".cut-line { text-align: center; font-size: 7pt; color: #aaa; margin-top: 6pt; letter-spacing: 1pt; }" +

                        /* ── Utilitaires ──────────────────────────────────────────── */
                        ".right { text-align: right; }";
    }

    // ════════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════════════════

    private String infoRow(String label, String value) {
        return "<tr><td>" + label + ":</td><td>" + value + "</td></tr>";
    }

    private String getBadgeCss(ReceiptType type) {
        if (type == null) return "badge-default";
        return switch (type) {
            case SHIFT_OPENING                -> "badge-opening";
            case SHIFT_CLOSING                -> "badge-closing";
            case CASH_IN, PAYMENT_RECEIVED    -> "badge-cashin";
            case CASH_OUT                     -> "badge-cashout";
            case REFUND                       -> "badge-refund";
            case CANCELLATION, VOID           -> "badge-cancel";
            default                           -> "badge-default";
        };
    }

    private String getAmountLabel(ReceiptType type) {
        if (type == null) return "MONTANT";
        return switch (type) {
            case SHIFT_OPENING    -> "Fond de caisse initial:";
            case SHIFT_CLOSING    -> "Total ventes session:";
            case CASH_IN          -> "Montant entr&#233; en caisse:";
            case CASH_OUT         -> "Montant sorti de caisse:";
            case PAYMENT_RECEIVED -> "Paiement re&#231;u:";
            case REFUND           -> "Montant rembours&#233;:";
            default               -> "MONTANT:";
        };
    }

    private String getAmountCss(ReceiptType type) {
        if (type == null) return "";
        return switch (type) {
            case SHIFT_OPENING, CASH_IN, PAYMENT_RECEIVED -> "amount-green";
            case CASH_OUT, REFUND, CANCELLATION, VOID     -> "amount-red";
            case SHIFT_CLOSING                            -> "amount-orange";
            default                                       -> "amount-blue";
        };
    }

    private String formatTypeLabel(ReceiptType type) {
        if (type == null) return "DOCUMENT DE CAISSE";
        return switch (type) {
            case SALE             -> "TICKET DE CAISSE";
            case REFUND           -> "TICKET DE REMBOURSEMENT";
            case CANCELLATION     -> "TICKET D'ANNULATION";
            case SHIFT_OPENING    -> "OUVERTURE DE CAISSE";
            case SHIFT_CLOSING    -> "FERMETURE DE CAISSE";
            case CASH_IN          -> "ENTR&#201;E D'ARGENT";
            case CASH_OUT         -> "SORTIE D'ARGENT";
            case PAYMENT_RECEIVED -> "RE&#199;U DE PAIEMENT";
            case DELIVERY_NOTE    -> "BON DE LIVRAISON";
            case VOID             -> "TICKET ANNUL&#201;";
        };
    }

    private String buildLogoTag() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(companyLogoPath);
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                String b64  = Base64.getEncoder().encodeToString(bytes);
                String mime = companyLogoPath.endsWith(".png") ? "image/png" : "image/jpeg";
                return "<img class='logo-img' src='data:" + mime + ";base64," + b64 + "'/>";
            }
        } catch (IOException e) {
            log.debug("Logo shift ticket non trouvé: {}", companyLogoPath);
        }
        return "";
    }

    private String fmtCur(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.0f FCFA", amount);
    }

    /**
     * CRITIQUE : échappe TOUS les caractères spéciaux XML/HTML.
     * À appeler sur CHAQUE valeur dynamique insérée dans le HTML.
     */
    protected String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}