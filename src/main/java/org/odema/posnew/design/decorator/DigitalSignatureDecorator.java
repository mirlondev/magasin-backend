package org.odema.posnew.design.decorator;


import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.DocumentBuilder;

/**
 * Ajoute une signature numérique au document
 */
@Slf4j
public class DigitalSignatureDecorator extends DocumentDecorator {

    private final String signerName;
    private final String signatureReason;

    public DigitalSignatureDecorator(DocumentBuilder builder,
                                     String signerName,
                                     String signatureReason) {
        super(builder);
        this.signerName = signerName;
        this.signatureReason = signatureReason;
    }

    @Override
    public byte[] build()  {
        byte[] originalPdf = super.build();

        // TODO: Implémenter signature numérique avec certificat
        // Pour l'instant, on retourne le PDF tel quel

        log.debug("Signature numérique: {} - {}", signerName, signatureReason);

        return originalPdf;
    }
}
