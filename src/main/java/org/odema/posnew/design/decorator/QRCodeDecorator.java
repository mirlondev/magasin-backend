package org.odema.posnew.design.decorator;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;

import java.io.ByteArrayOutputStream;

/**
 * Ajoute un QR code au document (pour scan mobile)
 */
@Slf4j
public class QRCodeDecorator extends DocumentDecorator {

    private final String qrCodeData;

    public QRCodeDecorator(DocumentBuilder builder, String qrCodeData) {
        super(builder);
        this.qrCodeData = qrCodeData;
    }

    @Override
    public byte[] build() {
        byte[] originalPdf = super.build();

        try {
            return addQRCode(originalPdf);
        } catch (Exception e) {
            log.error("Erreur ajout QR code", e);
            return originalPdf;
        }
    }

    private byte[] addQRCode(byte[] pdfBytes) throws Exception {
        PdfReader reader = new PdfReader(pdfBytes);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PdfStamper stamper = new PdfStamper(reader, output);

        // Générer le QR code
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
                qrCodeData,
                BarcodeFormat.QR_CODE,
                150, 150
        );

        ByteArrayOutputStream qrOutput = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrOutput);

        Image qrImage = Image.getInstance(qrOutput.toByteArray());
        qrImage.scaleToFit(100, 100);

        // Positionner en bas à droite de la première page
        Rectangle pageSize = reader.getPageSize(1);
        qrImage.setAbsolutePosition(
                pageSize.getWidth() - 120,
                20
        );

        PdfContentByte content = stamper.getOverContent(1);
        content.addImage(qrImage);

        stamper.close();
        reader.close();

        log.debug("QR code ajouté au document");

        return output.toByteArray();
    }
}
