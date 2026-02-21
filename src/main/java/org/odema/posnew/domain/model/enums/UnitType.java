package org.odema.posnew.domain.model.enums;

public enum UnitType {
    PIECE("pcs", 1.0),
    CARTON("carton", 12.0),    // 1 carton = 12 pcs
    KG("kg", 1000.0),          // base en grammes
    GRAM("g", 1.0),
    LITRE("L", 1000.0),        // base en ml
    ML("ml", 1.0),
    DOZEN("douzaine", 12.0),
    PACK("pack", 6.0);

    private final String label;
    private final double baseUnitFactor; // facteur vers l'unité de base

    UnitType(String label, double baseUnitFactor) {
        this.label = label;
        this.baseUnitFactor = baseUnitFactor;
    }

    public double toBaseUnits(double quantity) {
        return quantity * baseUnitFactor;
    }

    public double fromBaseUnits(double baseQuantity) {
        return baseQuantity / baseUnitFactor;
    }
}