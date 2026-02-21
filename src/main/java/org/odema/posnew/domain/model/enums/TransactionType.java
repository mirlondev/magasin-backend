package org.odema.posnew.domain.model.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
    SALE("Vente", true, true, false),
    REFUND("Remboursement", false, false, true),
    CASH_IN("Entrée caisse", true, false, false),
    CASH_OUT("Sortie caisse", false, false, false),
    ADJUSTMENT("Ajustement", true, false, false),
    CANCELLATION("Annulation", false, false, false);

    private final String label;
    private final boolean positive;
    private final boolean sale;
    private final boolean refund;

    TransactionType(String label, boolean positive, boolean sale, boolean refund) {
        this.label = label;
        this.positive = positive;
        this.sale = sale;
        this.refund = refund;
    }

    public boolean isNegative() {
        return !positive;
    }
}