package org.odema.posnew.domain.enums;

public enum InvoiceType {
    CREDIT_SALE("Facture vente"),
    PROFORMA("Proforma"),
    CREDIT_NOTE("Avoir"),
    DELIVERY_NOTE("Bon de livraison"),
    PURCHASE_ORDER("Bon de commande"),
    QUOTE("Devis"),
    INVOICE_CORRECTED("Facture rectificative");

    private final String label;

    InvoiceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isProforma() {
        return this == PROFORMA || this == QUOTE;
    }
}
