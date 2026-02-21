package org.odema.posnew.design.decorator;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Ajoute un watermark au document (BROUILLON, COPIE, etc.)
 */
@Slf4j
public class WatermarkDecorator extends DocumentDecorator {

    private final String watermarkText;
    private final BaseColor watermarkColor;

    public WatermarkDecorator(DocumentBuilder builder, String watermarkText) {
        super(builder);
        this.watermarkText = watermarkText;
        this.watermarkColor = new BaseColor(200, 200, 200, 100); // Gris transparent
    }

    @Override
    public byte[] build()  {
        // Construire le document de base
        byte[] originalPdf = super.build();

        try {
            // Ajouter le watermark
            return addWatermark(originalPdf);
        } catch (Exception e) {
            log.error("Erreur ajout watermark", e);
            return originalPdf; // Retourner le PDF sans watermark en cas d'erreur
        }
    }

    private byte[] addWatermark(byte[] pdfBytes) throws Exception {
        PdfReader reader = new PdfReader(pdfBytes);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfStamper stamper = new PdfStamper(reader, output);

        // Font pour le watermark
        Font watermarkFont = new Font(Font.FontFamily.HELVETICA, 60,
                Font.BOLD, watermarkColor);

        int totalPages = reader.getNumberOfPages();

        for (int i = 1; i <= totalPages; i++) {
            PdfContentByte content = stamper.getOverContent(i);

            // Obtenir les dimensions de la page
            Rectangle pageSize = reader.getPageSize(i);
            float x = pageSize.getWidth() / 2;
            float y = pageSize.getHeight() / 2;

            // Ajouter le texte en diagonale
            ColumnText.showTextAligned(
                    content,
                    Element.ALIGN_CENTER,
                    new Phrase(watermarkText, watermarkFont),
                    x, y, 45 // rotation 45°
            );
        }

        stamper.close();
        reader.close();

        log.debug("Watermark '{}' ajouté au document", watermarkText);

        return output.toByteArray();
    }
}
