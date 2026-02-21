package org.odema.posnew.design.builder;



public interface DocumentBuilder {
    /**
     * Initialise le document
     */
    DocumentBuilder initialize();

    /**
     * Ajoute l'en-tête
     */
    DocumentBuilder addHeader();

    /**
     * Ajoute les informations principales
     */
    DocumentBuilder addMainInfo();

    /**
     * Ajoute le tableau des articles
     */
    DocumentBuilder addItemsTable();

    /**
     * Ajoute les totaux
     */
    DocumentBuilder addTotals();

    /**
     * Ajoute le pied de page
     */
    DocumentBuilder addFooter();

    /**
     * Construit et retourne le document final
     */
    byte[] build();
}
