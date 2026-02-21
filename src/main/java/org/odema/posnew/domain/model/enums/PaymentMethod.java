package org.odema.posnew.domain.model.enums;

import lombok.Getter;

@Getter
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

    public boolean isImmediatePayment() {
        return this != CREDIT;
    }
}
