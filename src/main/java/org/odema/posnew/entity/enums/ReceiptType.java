package org.odema.posnew.entity.enums;

public enum ReceiptType {
    SALE,              // Vente normale (POS)
    REFUND,            // Remboursement
    CANCELLATION,      // Annulation
    SHIFT_OPENING,     // Ouverture de caisse
    SHIFT_CLOSING,     // Fermeture de caisse
    CASH_IN,           // Entrée d'argent
    CASH_OUT,          // Sortie d'argent
    PAYMENT_RECEIVED,  // Paiement reçu (pour crédit)
    DELIVERY_NOTE,     // Bon de livraison
    VOID               // Ticket annulé
}
