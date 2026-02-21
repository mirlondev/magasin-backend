package org.odema.posnew.design.template;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.context.DocumentBuildContext;
import org.odema.posnew.design.factory.DocumentBuilderFactory;
import org.odema.posnew.design.factory.DocumentStrategyFactory;
import org.odema.posnew.design.strategy.DocumentStrategy;
import org.odema.posnew.design.strategy.ValidationResult;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.enums.DocumentType;
import org.odema.posnew.domain.repository.OrderRepository;
import org.odema.posnew.domain.service.DocumentNumberService;
import org.odema.posnew.domain.service.FileStorageService;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

/**
 * Template Method pour la génération de documents.
 *
 * Nouvelle approche :
 *  - generatePdfDocument() a une implémentation par défaut via DocumentBuilderFactory
 *  - Les sous-classes surchargent UNIQUEMENT si elles ont besoin de décorateurs
 *    ou d'une logique spéciale (shift receipts, watermark, QR code...)
 *  - Plus de new XxxBuilder() dans les services
 */
@Slf4j
@RequiredArgsConstructor
public abstract class DocumentServiceTemplate<T, R> {

    protected final OrderRepository orderRepository;
    protected final DocumentStrategyFactory strategyFactory;
    protected final DocumentNumberService documentNumberService;
    protected final FileStorageService fileStorageService;
    protected final DocumentBuilderFactory builderFactory;  // ✅ injecté ici

    // =========================================================================
    // TEMPLATE METHOD — génération
    // =========================================================================

    @Transactional
    public R generateDocument(UUID orderId) throws IOException {
        log.info("Génération document pour commande {}", orderId);

        // 1. Charger la commande
        Order order = loadOrder(orderId);

        // 2. Vérifier qu'un document n'existe pas déjà
        checkExistingDocument(order);

        // 3. Récupérer la stratégie appropriée
        DocumentStrategy strategy = strategyFactory.getStrategyForOrder(order);

        // 4. Valider
        ValidationResult validation = strategy.validateForGeneration(order);
        if (!validation.isValid()) {
            throw new BadRequestException(
                    "Validation document échouée: " +
                            String.join(", ", validation.getErrors())
            );
        }

        // 5. Préparer les données
        strategy.prepareDocumentData(order);

        // 6. Générer le numéro unique
        String documentNumber = generateUniqueDocumentNumber(order, strategy);

        // 7. Créer l'entité
        T document = createDocumentEntity(order, strategy, documentNumber);

        // 8. Sauvegarder pour obtenir l'ID
        T savedDocument = saveDocument(document);
        log.debug("Document sauvegardé avec ID: {}", getDocumentId(savedDocument));

        // 9. ✅ Générer le PDF via la factory — plus de new Builder() dans les services
        byte[] pdfBytes = generatePdfDocument(savedDocument, strategy);

        // 10. Sauvegarder le PDF sur disque
        String pdfPath = savePdfDocument(savedDocument, pdfBytes, documentNumber);

        // 11. Mettre à jour le chemin PDF
        updateDocumentWithPdfPath(savedDocument, pdfPath);

        // 12. Re-sauvegarder avec le chemin
        savedDocument = saveDocument(savedDocument);

        // 13. Hook post-génération
        savedDocument = afterDocumentGeneration(savedDocument, order, strategy);

        // 14. Publier événement
        publishDocumentEvent(savedDocument, order);

        log.info("Document {} généré avec succès", documentNumber);

        return mapToResponse(savedDocument);
    }

    // =========================================================================
    // TEMPLATE METHOD — réimpression
    // =========================================================================

    @Transactional
    public R reprintDocument(UUID documentId) throws IOException {
        log.info("Réimpression document {}", documentId);

        T document = loadDocument(documentId);
        checkReprintAllowed(document);
        incrementPrintCount(document);
        updateReprintStatus(document);
        T savedDocument = saveDocument(document);
        logReprint(savedDocument);

        return mapToResponse(savedDocument);
    }

    // =========================================================================
    // MÉTHODE CONCRÈTE — génération PDF via factory (comportement par défaut)
    //
    // Les sous-classes surchargent cette méthode UNIQUEMENT si :
    //   - décorateurs nécessaires (WatermarkDecorator, QRCodeDecorator)
    //   - logique spéciale (ShiftReceipt qui n'a pas de DocumentType mappable)
    // =========================================================================

    protected byte[] generatePdfDocument(T document, DocumentStrategy strategy) throws IOException {

        // Résoudre le DocumentType depuis la stratégie
        DocumentType docType = strategy.getDocumentType();

        // Construire le contexte depuis l'entité document
        DocumentBuildContext ctx = buildContext(document);

        // Déléguer entièrement à la factory
        return builderFactory.createBuilder(docType, ctx)
                .initialize()
                .addHeader()
                .addMainInfo()
                .addItemsTable()
                .addTotals()
                .addFooter()
                .build();
    }

    /**
     * Construit le DocumentBuildContext depuis l'entité document.
     *
     * Implémentation par défaut : lance une exception — force les sous-classes
     * à fournir le contexte approprié selon leur type d'entité (Receipt, Invoice...).
     *
     * Si la sous-classe surcharge generatePdfDocument() entièrement,
     * elle n'a pas besoin de surcharger buildContext().
     */
    protected DocumentBuildContext buildContext(T document) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + " doit surcharger buildContext() " +
                        "ou surcharger generatePdfDocument() entièrement."
        );
    }

    // =========================================================================
    // MÉTHODES ABSTRAITES — à implémenter dans chaque sous-classe
    // =========================================================================

    protected abstract void checkExistingDocument(Order order);

    protected abstract T createDocumentEntity(Order order,
                                              DocumentStrategy strategy,
                                              String documentNumber);

    protected abstract void updateDocumentWithPdfPath(T document, String pdfPath);

    protected abstract T afterDocumentGeneration(T document,
                                                 Order order,
                                                 DocumentStrategy strategy);

    protected abstract T saveDocument(T document);

    protected abstract void publishDocumentEvent(T document, Order order);

    protected abstract R mapToResponse(T document);

    protected abstract T loadDocument(UUID documentId);

    protected abstract void checkReprintAllowed(T document);

    protected abstract void incrementPrintCount(T document);

    protected abstract void updateReprintStatus(T document);

    protected abstract void logReprint(T document);

    protected abstract String getStorageDirectory();

    protected abstract UUID getDocumentId(T document);

    // =========================================================================
    // MÉTHODES CONCRÈTES COMMUNES
    // =========================================================================

    protected Order loadOrder(UUID orderId) {
        return orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new BadRequestException("Commande non trouvée"));
    }

    /**
     * Implémentation par défaut retourne null — les sous-classes DOIVENT surcharger.
     * Déclaré concret (pas abstrait) pour éviter de casser les sous-classes existantes
     * pendant la migration, mais un log warn est émis si null est retourné.
     */
    protected String generateUniqueDocumentNumber(Order order, DocumentStrategy strategy) {
        log.warn("{} n'implémente pas generateUniqueDocumentNumber() — " +
                "le numéro de document sera null !", getClass().getSimpleName());
        return null;
    }

    protected String savePdfDocument(T document, byte[] pdfBytes, String documentNumber)
            throws IOException {
        String filename  = documentNumber + ".pdf";
        String directory = getStorageDirectory();
        fileStorageService.storeFileFromBytes(pdfBytes, filename, directory);
        return directory + "/" + filename;
    }
}