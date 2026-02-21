package org.odema.posnew.domain.model.enums;

import lombok.Getter;

@Getter
public enum DocumentType {
    TICKET("Ticket de caisse", "RCP"),
    INVOICE("Facture", "INV"),
    PROFORMA("Proforma", "PRO"),
    REFUND("Remboursement", "RMB"),
    CREDIT_NOTE("Note de crédit", "AVO"),
    DELIVERY_NOTE("Bon de livraison", "BL"),
    RECEIPT("Reçu", "REC"),
    SHIFT_REPORT("Rapport de caisse", "RPT"),
    CANCELLATION("Annulation", "ANN"),
    VOID("Annulation", "VOID");

    private final String label;
    private final String prefix;

    DocumentType(String label, String prefix) {
        this.label = label;
        this.prefix = prefix;
    }

}
