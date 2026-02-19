package org.odema.posnew.entity.enums;

/**
 * Méthodes de remboursement disponibles.
 */
public enum RefundMethod {
    CASH("Espèces", "Remboursement immédiat en espèces"),
    CREDIT_CARD("Carte bancaire", "Remboursement sur la carte utilisée"),
    MOBILE_MONEY("Mobile Money", "Remboursement via Mobile Money"),
    BANK_TRANSFER("Virement bancaire", "Virement sur compte bancaire"),
    STORE_CREDIT("Avoir magasin", "Crédit utilisable en magasin"),
    ORIGINAL_PAYMENT_METHOD("Même mode de paiement", "Remboursement sur le mode de paiement original");

    private final String label;
    private final String description;

    RefundMethod(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
