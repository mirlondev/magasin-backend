package org.odema.posnew.domain.model.enums;

public enum RefundMethod {
    CASH("Espèces"),
    CREDIT_CARD("Remboursement carte"),
    MOBILE_MONEY("Mobile Money"),
    BANK_TRANSFER("Virement bancaire"),
    STORE_CREDIT("Avoir magasin"),
    ORIGINAL_PAYMENT_METHOD("Methode de paiement initiale");
    private final String label;

    RefundMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
