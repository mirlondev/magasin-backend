package org.odema.posnew.domain.enums;

public enum InvoiceStatus {
    DRAFT("Brouillon"),
    ISSUED("Émise"),
    PAID("Payée"),
    OVERDUE("En retard"),
    CANCELLED("Annulée"),
    CONVERTED("Convertie");

    private final String label;

    InvoiceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
