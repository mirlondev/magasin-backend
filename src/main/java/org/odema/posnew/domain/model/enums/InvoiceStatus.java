package org.odema.posnew.domain.model.enums;

import lombok.Getter;

@Getter
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

}
