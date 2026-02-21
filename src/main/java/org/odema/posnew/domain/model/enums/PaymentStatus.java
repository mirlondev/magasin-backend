package org.odema.posnew.domain.enums;

public enum PaymentStatus {
    UNPAID("Non payé"),
    PARTIALLY_PAID("Partiellement payé"),
    PAID("Payé"),
    CREDIT("Crédit"),
    REFUNDED("Remboursé"),
    CANCELLED("Annulé");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isFinal() {
        return this == PAID || this == CREDIT || this == REFUNDED || this == CANCELLED;
    }
}
