package org.odema.posnew.domain.enums;

public enum TransactionType {
    SALE("Vente", true),
    REFUND("Remboursement", false),
    CASH_IN("Entrée caisse", true),
    CASH_OUT("Sortie caisse", false),
    ADJUSTMENT("Ajustement", true),
    CANCELLATION("Annulation", false);

    private final String label;
    private final boolean positive;

    TransactionType(String label, boolean positive) {
        this.label = label;
        this.positive = positive;
    }

    public String getLabel() {
        return label;
    }

    public boolean isPositive() {
        return positive;
    }

    public boolean isNegative() {
        return !positive;
    }
}
