package org.odema.posnew.design.strategy;

import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.enums.DocumentType;

/**
 * Interface du pattern Strategy pour la génération de documents.
 * Chaque stratégie définit comment générer un type spécifique de document.
 */
public interface DocumentStrategy {

    /**
     * Vérifie si cette stratégie peut générer un document pour la commande donnée
     */
    boolean canGenerate(Order order);

    /**
     * Retourne le type de document géré par cette stratégie
     */
    DocumentType getDocumentType();

    /**
     * Valide que la commande peut recevoir un document de ce type
     */
    ValidationResult validateForGeneration(Order order);

    /**
     * Prépare les données spécifiques avant la génération
     */
    void prepareDocumentData(Order order);

    /**
     * Génère le numéro de document
     */
    String generateDocumentNumber();

    /**
     * Indique si ce type de document autorise les réimpressions
     */
    boolean allowsReprint();

    /**
     * Indique si ce type de document peut être annulé
     */
    boolean allowsVoid();
}
