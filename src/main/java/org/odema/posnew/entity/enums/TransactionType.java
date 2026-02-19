package org.odema.posnew.entity.enums;

/**
 * Types de transactions financières.
 */
public enum TransactionType {
    // Ventes
    SALE("Vente", "Transaction de vente", true),
    SALE_CASH("Vente espèces", "Vente payée en espèces", true),
    SALE_CARD("Vente carte", "Vente payée par carte", true),
    SALE_MOBILE("Vente mobile", "Vente payée par Mobile Money", true),
    SALE_CREDIT("Vente crédit", "Vente à crédit", true),

    // Remboursements
    REFUND("Remboursement", "Remboursement client", false),
    REFUND_CASH("Remboursement espèces", "Remboursement en espèces", false),
    REFUND_CARD("Remboursement carte", "Remboursement sur carte", false),

    // Mouvements de caisse
    CASH_IN("Entrée caisse", "Entrée d'argent en caisse", true),
    CASH_OUT("Sortie caisse", "Sortie d'argent de caisse", false),
    OPENING_BALANCE("Fond caisse", "Fond de caisse initial", true),
    CLOSING_BALANCE("Clôture caisse", "Solde de clôture", false),

    // Ajustements
    ADJUSTMENT("Ajustement", "Ajustement de caisse", true),
    DISCREPANCY_POSITIVE("Écart +", "Écart positif", true),
    DISCREPANCY_NEGATIVE("Écart -", "Écart négatif", false),

    // Autres
    DEPOSIT("Dépôt", "Dépôt bancaire", false),
    WITHDRAWAL("Retrait", "Retrait bancaire", true),
    TRANSFER("Transfert", "Transfert entre caisses", true),
    EXPENSE("Dépense", "Dépense magasin", false),
    INCOME("Revenu", "Revenu divers", true);

    private final String label;
    private final String description;
    private final boolean positive; // true = entrée d'argent, false = sortie

    TransactionType(String label, String description, boolean positive) {
        this.label = label;
        this.description = description;
        this.positive = positive;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPositive() {
        return positive;
    }

    public boolean isNegative() {
        return !positive;
    }

    public boolean isSale() {
        return name().startsWith("SALE");
    }

    public boolean isRefund() {
        return name().startsWith("REFUND");
    }

    public boolean isCashMovement() {
        return this == CASH_IN || this == CASH_OUT;
    }
}
