package org.odema.posnew.domain.enums;

public enum RefundMethod {
    CASH("Espèces"),
    CREDIT_CARD("Remboursement carte"),
    MOBILE_MONEY("Mobile Money"),
    BANK_TRANSFER("Virement bancaire"),
    STORE_CREDIT("Avoir magasin");

    private final String label;

    RefundMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
