package org.odema.posnew.domain.enums;

public enum RefundType {
    FULL("Remboursement total"),
    PARTIAL("Remboursement partiel"),
    EXCHANGE("Échange");

    private final String label;

    RefundType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
