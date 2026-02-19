package org.odema.posnew.entity.enums;

/**
 * Types de documents générables par le système.
 */
public enum DocumentType {
    // Documents de vente
    TICKET("Ticket de caisse", "RCP"),
    INVOICE("Facture", "INV"),
    PROFORMA("Proforma", "PRO"),
    
    // Documents de retour/remboursement
    REFUND("Remboursement", "RMB"),
    CREDIT_NOTE("Note de crédit", "AVO"),
    
    // Documents logistiques
    DELIVERY_NOTE("Bon de livraison", "BL"),
    
    // Documents de caisse
    RECEIPT("Reçu", "REC"),
    SHIFT_REPORT("Rapport de caisse", "RPT"),
    
    // Documents spéciaux
    CANCELLATION("Annulation", "ANN"),
    VOID("Annulation", "VOID");

    private final String label;
    private final String prefix;

    DocumentType(String label, String prefix) {
        this.label = label;
        this.prefix = prefix;
    }

    public String getLabel() {
        return label;
    }

    public String getPrefix() {
        return prefix;
    }
}
