package org.odema.posnew.design.template;


import com.itextpdf.text.DocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.odema.posnew.design.factory.DocumentStrategyFactory;
import org.odema.posnew.design.strategy.DocumentStrategy;
import org.odema.posnew.design.strategy.ValidationResult;
import org.odema.posnew.entity.Order;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.repository.OrderRepository;
import org.odema.posnew.service.DocumentNumberService;
import org.odema.posnew.service.FileStorageService;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Template Method pour la génération de documents
 */
@Slf4j
@RequiredArgsConstructor
public abstract class DocumentServiceTemplate<T, R> {

    protected final OrderRepository orderRepository;
    protected final DocumentStrategyFactory strategyFactory;
    protected final DocumentNumberService documentNumberService;
    protected final FileStorageService fileStorageService;

    /**
     * TEMPLATE METHOD - Squelette de génération de document
     */
    @Transactional
    public R generateDocument(UUID orderId) throws IOException, DocumentException {
        log.info("Génération document pour commande {}", orderId);

        // 1. Charger la commande
        Order order = loadOrder(orderId);

        // 2. Vérifier qu'un document n'existe pas déjà
        checkExistingDocument(order);

        // 3. Récupérer la stratégie appropriée
        DocumentStrategy strategy = strategyFactory.getStrategyForOrder(order);

        // 4. Valider selon la stratégie
        ValidationResult validation = strategy.validateForGeneration(order);
        if (!validation.isValid()) {
            throw new BadRequestException(
                    "Validation document échouée: " +
                            String.join(", ", validation.getErrors())
            );
        }

        // 5. Préparer les données
        strategy.prepareDocumentData(order);

        // 6. Générer le numéro de document unique
        String documentNumber = generateUniqueDocumentNumber(order, strategy);

        // 7. Créer l'entité document
        T document = createDocumentEntity(order, strategy, documentNumber);

        // 8. Générer le PDF
        byte[] pdfBytes = generatePdfDocument(document, strategy);

        // 9. Sauvegarder le PDF
        String pdfPath = savePdfDocument(document, pdfBytes, documentNumber);

        // 10. Mettre à jour l'entité avec le chemin PDF
        updateDocumentWithPdfPath(document, pdfPath);

        // 11. Hook: Traitement post-génération
        T savedDocument = afterDocumentGeneration(document, order, strategy);

        // 12. Sauvegarder l'entité finale
        savedDocument = saveDocument(savedDocument);

        // 13. Publier événement
        publishDocumentEvent(savedDocument, order);

        log.info("Document {} généré avec succès", documentNumber);

        return mapToResponse(savedDocument);
    }

    /**
     * TEMPLATE METHOD - Réimpression de document
     */
    @Transactional
    public R reprintDocument(UUID documentId) throws IOException {
        log.info("Réimpression document {}", documentId);

        // 1. Charger le document
        T document = loadDocument(documentId);

        // 2. Vérifier si réimpression autorisée
        checkReprintAllowed(document);

        // 3. Incrémenter compteur
        incrementPrintCount(document);

        // 4. Mettre à jour statut
        updateReprintStatus(document);

        // 5. Sauvegarder
        T savedDocument = saveDocument(document);

        // 6. Logger
        logReprint(savedDocument);

        return mapToResponse(savedDocument);
    }

    // ========== MÉTHODES ABSTRAITES (à implémenter) ==========

    /**
     * Vérifie si un document existe déjà
     */
    protected abstract void checkExistingDocument(Order order);

    /**
     * Crée l'entité document
     */
    protected abstract T createDocumentEntity(Order order,
                                              DocumentStrategy strategy,
                                              String documentNumber);

    /**
     * Génère le PDF du document
     */
    protected abstract byte[] generatePdfDocument(T document,
                                                  DocumentStrategy strategy) throws DocumentException;

    /**
     * Met à jour l'entité avec le chemin PDF
     */
    protected abstract void updateDocumentWithPdfPath(T document, String pdfPath);

    /**
     * Hook après génération
     */
    protected abstract T afterDocumentGeneration(T document, Order order,
                                                 DocumentStrategy strategy);

    /**
     * Sauvegarde le document
     */
    protected abstract T saveDocument(T document);

    /**
     * Publie l'événement
     */
    protected abstract void publishDocumentEvent(T document, Order order);

    /**
     * Mappe vers le DTO de réponse
     */
    protected abstract R mapToResponse(T document);

    /**
     * Charge un document par ID
     */
    protected abstract T loadDocument(UUID documentId);

    /**
     * Vérifie si réimpression autorisée
     */
    protected abstract void checkReprintAllowed(T document);

    /**
     * Incrémente le compteur d'impressions
     */
    protected abstract void incrementPrintCount(T document);

    /**
     * Met à jour le statut lors de réimpression
     */
    protected abstract void updateReprintStatus(T document);

    /**
     * Log la réimpression
     */
    protected abstract void logReprint(T document);

    /**
     * Répertoire de stockage des PDFs
     */
    protected abstract String getStorageDirectory();

    // ========== MÉTHODES CONCRÈTES COMMUNES ==========

    protected Order loadOrder(UUID orderId) {
        return orderRepository.findByIdWithPayments(orderId)
                .orElseThrow(() -> new BadRequestException("Commande non trouvée"));
    }

    protected String generateUniqueDocumentNumber(Order order, DocumentStrategy strategy) {
        // Implémentation dépendant du type de document
        return null; // À surcharger dans les sous-classes si nécessaire
    }

    protected String savePdfDocument(T document, byte[] pdfBytes,
                                     String documentNumber) throws IOException {
        String filename = documentNumber + ".pdf";
        String directory = getStorageDirectory();

        fileStorageService.storeFileFromBytes(pdfBytes, filename, directory);

        return directory + "/" + filename;
    }
}