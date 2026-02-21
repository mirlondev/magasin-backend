package org.odema.posnew.domain.enums;

public enum PaymentMethod {
    CASH("Espèces"),
    CREDIT_CARD("Carte bancaire"),
    MOBILE_MONEY("Mobile Money"),
    BANK_TRANSFER("Virement bancaire"),
    CHECK("Chèque"),
    LOYALTY_POINTS("Points fidélité"),
    CREDIT("Crédit client");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isImmediatePayment() {
        return this != CREDIT;
    }
}
