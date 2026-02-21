package org.odema.posnew.domain.enums;

public enum OrderStatus {
    PENDING("En attente"),
    CONFIRMED("Confirmé"),
    PROCESSING("En traitement"),
    COMPLETED("Terminé"),
    CANCELLED("Annulé"),
    REFUNDED("Remboursé");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean canAddPayment() {
        return this == PENDING || this == CONFIRMED || this == PROCESSING;
    }

    public boolean isModifiable() {
        return this == PENDING || this == CONFIRMED;
    }
}
