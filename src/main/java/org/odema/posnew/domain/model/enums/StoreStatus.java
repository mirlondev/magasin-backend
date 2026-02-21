package org.odema.posnew.domain.enums;

public enum StoreStatus {
    ACTIVE("Actif"),
    PENDING("En attente"),
    CLOSED("Fermé"),
    SUSPENDED("Suspendu");

    private final String label;

    StoreStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
