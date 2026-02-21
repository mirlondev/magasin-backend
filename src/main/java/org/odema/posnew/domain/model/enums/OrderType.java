package org.odema.posnew.domain.enums;

public enum OrderType {
    POS_SALE("Vente magasin"),
    CREDIT_SALE("Vente à crédit"),
    PROFORMA("Proforma/Devis"),
    ONLINE("Commande en ligne"),
    RETURN("Retour"),
    EXCHANGE("Échange");

    private final String label;

    OrderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean requiresImmediatePayment() {
        return this == POS_SALE || this == ONLINE;
    }

    public boolean allowsCredit() {
        return this == CREDIT_SALE || this == PROFORMA;
    }
}
