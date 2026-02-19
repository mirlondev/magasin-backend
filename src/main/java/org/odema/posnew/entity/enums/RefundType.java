package org.odema.posnew.entity.enums;

/**
 * Types de remboursement.
 */
public enum RefundType {
    FULL("Total", "Remboursement complet de la commande"),
    PARTIAL("Partiel", "Remboursement partiel (certains articles)"),
    EXCHANGE("Échange", "Remboursement avec échange de produit"),
    STORE_CREDIT("Avoir", "Crédit magasin sans remboursement cash"),
    WARRANTY("Garantie", "Remboursement sous garantie"),
    DEFECTIVE("Défectueux", "Remboursement pour produit défectueux"),
    WRONG_ITEM("Erreur article", "Remboursement pour article erroné"),
    CUSTOMER_REQUEST("Demande client", "Remboursement sur demande du client");

    private final String label;
    private final String description;

    RefundType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean requiresItemReturn() {
        return this == EXCHANGE || this == DEFECTIVE || this == WRONG_ITEM;
    }
}
