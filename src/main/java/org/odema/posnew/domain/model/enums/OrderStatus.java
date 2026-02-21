package org.odema.posnew.domain.model.enums;

import lombok.Getter;

@Getter
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

    public boolean canAddPayment() {
        return this == PENDING || this == CONFIRMED || this == PROCESSING;
    }

    public boolean isModifiable() {
        return this == PENDING || this == CONFIRMED;
    }
}
