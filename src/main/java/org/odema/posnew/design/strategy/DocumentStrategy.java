package org.odema.posnew.design.strategy;
// strategy/DocumentStrategy.java


import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.enums.DocumentType;

/**
 * Strategy pour déterminer le type de document à générer
 */
public interface DocumentStrategy {
    /**
     * Détermine si cette stratégie peut générer un document pour cette commande
     */
    boolean canGenerate(Order order);

    /**
     * Type de document généré par cette stratégie
     */
    DocumentType getDocumentType();

    /**
     * Valide si le document peut être créé
     */
    ValidationResult validateForGeneration(Order order);

    /**
     * Prépare les données spécifiques avant génération
     */
    void prepareDocumentData(Order order);

    /**
     * Numéro de document unique
     */
    String generateDocumentNumber();

    /**
     * Le document peut-il être réimprimé
     */
    boolean allowsReprint();

    /**
     * Le document peut-il être annulé
     */
    boolean allowsVoid();
}

