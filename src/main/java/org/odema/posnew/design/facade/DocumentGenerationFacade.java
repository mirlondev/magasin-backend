package org.odema.posnew.design.facade;

import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.builder.impl.InvoiceDocumentBuilder;
import org.odema.posnew.design.builder.impl.ReceiptDocumentBuilder;
import org.odema.posnew.entity.enums.DocumentType;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.service.FileStorageService;
import org.springframework.stereotype.Component;
import  org.odema.posnew.entity.Order;
import java.io.IOException;

/**
 * Facade qui simplifie la génération de documents
 * Cache la complexité des builders, du stockage fichier, etc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentGenerationFacade {

    private final InvoiceDocumentBuilder invoiceBuilder;
    private final ReceiptDocumentBuilder receiptBuilder;
    private final FileStorageService fileStorageService;

    /**
     * Génère un document selon le type et retourne le contenu PDF
     */
    public byte[] generateDocument(Order order, DocumentType documentType) {
        log.info("Génération document {} pour commande {}",
                documentType, order.getOrderNumber());

        try {
            return switch (documentType) {
                case INVOICE -> generateInvoice(order);
                case TICKET, RECEIPT -> generateReceipt(order);
                case PROFORMA-> generateProforma(order);
            };
        } catch (Exception e) {
            log.error("Erreur génération document", e);
            throw new BadRequestException("Erreur génération document: " + e.getMessage());
        }
    }

    /**
     * Génère ET sauvegarde le document, retourne le chemin fichier
     */
    public String generateAndSaveDocument(Order order, DocumentType documentType,
                                          String directory) throws IOException {
        byte[] pdfBytes = generateDocument(order, documentType);

        String filename = buildFilename(order, documentType);

        fileStorageService.storeFileFromBytes(pdfBytes, filename, directory);

        log.info("Document sauvegardé: {}/{}", directory, filename);

        return directory + "/" + filename;
    }

    private byte[] generateInvoice(Order order) throws DocumentException {
        return new InvoiceDocumentBuilder(order)
                .initialize()
                .addHeader()
                .addMainInfo()
                .addItemsTable()
                .addTotals()
                .addFooter()
                .build();
    }

    private byte[] generateReceipt(Order order) throws DocumentException {
        return new ReceiptDocumentBuilder(order)
                .initialize()
                .addHeader()
                .addMainInfo()
                .addItemsTable()
                .addTotals()
                .addFooter()
                .build();
    }

    private byte[] generateProforma(Order order) throws DocumentException {
        // Proforma utilise le même template qu'Invoice mais avec titre différent
        // TODO: Créer ProformaDocumentBuilder si besoin de différences
        return new InvoiceDocumentBuilder(order)
                .initialize()
                .addHeader()
                .addMainInfo()
                .addItemsTable()
                .addTotals()
                .addFooter()
                .build();
    }

    private String buildFilename(Order order, DocumentType documentType) {
        String prefix = switch (documentType) {
            case INVOICE -> "facture";
            case TICKET, RECEIPT -> "ticket";
            case PROFORMA -> "proforma";
        };

        return String.format("%s-%s.pdf", prefix, order.getOrderNumber());
    }
}