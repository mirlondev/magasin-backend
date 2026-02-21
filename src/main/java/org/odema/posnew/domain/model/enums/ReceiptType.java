package org.odema.posnew.domain.enums;

public enum ReceiptType {
    SALE("Vente"),
    REFUND("Remboursement"),
    CANCELLATION("Annulation"),
    SHIFT_OPENING("Ouverture caisse"),
    SHIFT_CLOSING("Fermeture caisse"),
    CASH_IN("Entrée caisse"),
    CASH_OUT("Sortie caisse"),
    PAYMENT_RECEIVED("Paiement reçu"),
    DELIVERY_NOTE("Bon de livraison"),
    VOID("Ticket annulé");

    private final String label;

    ReceiptType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isShiftRelated() {
        return this == SHIFT_OPENING || this == SHIFT_CLOSING ||
                this == CASH_IN || this == CASH_OUT;
    }
}
