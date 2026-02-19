package org.odema.posnew.entity.enums;

/**
 * Statuts possibles d'un remboursement.
 */
public enum RefundStatus {
    PENDING("En attente", "Le remboursement est en attente de validation"),
    APPROVED("Approuvé", "Le remboursement a été approuvé"),
    PROCESSING("En cours", "Le remboursement est en cours de traitement"),
    COMPLETED("Terminé", "Le remboursement a été effectué"),
    REJECTED("Rejeté", "Le remboursement a été rejeté"),
    CANCELLED("Annulé", "Le remboursement a été annulé"),
    FAILED("Échoué", "Le remboursement a échoué");

    private final String label;
    private final String description;

    RefundStatus(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == REJECTED || this == CANCELLED || this == FAILED;
    }

    public boolean canBeProcessed() {
        return this == PENDING || this == APPROVED;
    }
}
