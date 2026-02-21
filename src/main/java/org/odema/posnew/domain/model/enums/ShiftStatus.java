package org.odema.posnew.domain.model.enums;

public enum ShiftStatus {
    OPEN("Ouvert"),
    SUSPENDED("Suspendu"),
    CLOSED("Fermé");

    private final String label;

    ShiftStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
