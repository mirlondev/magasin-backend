package org.odema.posnew.domain.enums;

public enum StockStatus {
    IN_STOCK("En stock"),
    LOW_STOCK("Stock faible"),
    OUT_OF_STOCK("Rupture"),
    OVER_STOCK("Surstock");

    private final String label;

    StockStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
