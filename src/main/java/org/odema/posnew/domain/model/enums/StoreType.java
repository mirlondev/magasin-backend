package org.odema.posnew.domain.enums;

public enum StoreType {
    SHOP("Boutique"),
    WAREHOUSE("Dépôt/Entrepôt"),
    KIOSK("Kiosque"),
    POPUP("Magasin temporaire");

    private final String label;

    StoreType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean hasInventoryManagement() {
        return this == SHOP || this == WAREHOUSE || this == KIOSK;
    }
}
