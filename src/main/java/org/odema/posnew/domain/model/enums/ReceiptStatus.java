package org.odema.posnew.domain.enums;

public enum ReceiptStatus {
    ACTIVE("Actif"),
    REPRINTED("Réimprimé"),
    VOID("Annulé");

    private final String label;

    ReceiptStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
