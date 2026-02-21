package org.odema.posnew.design.builder.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;
import org.odema.posnew.domain.model.Order;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

/**
 * Classe abstraite de base pour tous les builders PDF.
 * Utilise exclusivement openhtmltopdf — iText est supprimé.
 *
 * Responsabilités :
 *  - Utilitaires partagés : esc(), formatCurrency(), xhtmlDocStart(), renderHtmlToPdf()
 *  - Contrat minimal : outputStream + order
 *
 * Ce que cette classe NE fait PAS :
 *  - Pas de config @Value (chaque builder reçoit sa config via withConfig())
 *  - Pas de initialize() ni build() par défaut (logique propre à chaque builder)
 */
@Slf4j
public abstract class AbstractPdfDocumentBuilder implements DocumentBuilder {

    protected ByteArrayOutputStream outputStream;
    protected Order order;

    protected AbstractPdfDocumentBuilder(Order order) {
        this.order = order;
    }

    // ═══════════════════════════════════════════════════
    // UTILITAIRES HTML/XHTML communs
    // ═══════════════════════════════════════════════════

    /**
     * Génère le début du document XHTML valide pour openhtmltopdf.
     * Appeler en début de initialize() dans chaque sous-classe.
     */
    protected String xhtmlDocStart(String css) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
                "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"fr\" lang=\"fr\">" +
                "<head>" +
                "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>" +
                "<style>" + css + "</style>" +
                "</head><body>";
    }

    /**
     * Rend le HTML en PDF via openhtmltopdf.
     * Appeler dans build() de chaque sous-classe.
     */
    protected byte[] renderHtmlToPdf(String html) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            log.debug("PDF rendu: {} octets", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Erreur rendu PDF: {}", e.getMessage());
            throw new RuntimeException("Échec génération PDF", e);
        }
    }

    /**
     * Échappe TOUS les caractères spéciaux XML/HTML.
     * Obligatoire sur toute valeur dynamique insérée dans le HTML.
     * Le & DOIT être remplacé EN PREMIER pour éviter le double-échappement.
     */
    protected String esc(String s) {
        if (s == null) return "";
        return s.replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;")
                .replace("'",  "&#39;");
    }

    /**
     * Formate un montant en FCFA.
     */
    protected String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,.0f FCFA", amount);
    }
}