package org.odema.posnew.domain.enums;

public enum RefundStatus {
    PENDING("En attente"),
    APPROVED("Approuvé"),
    PROCESSING("En traitement"),
    COMPLETED("Terminé"),
    REJECTED("Rejeté"),
    CANCELLED("Annulé"),
    FAILED("Échoué");

    private final String label;

    RefundStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isFinal() {
        return this == COMPLETED || this == REJECTED || this == CANCELLED || this == FAILED;
    }

    public boolean canTransitionTo(RefundStatus next) {
        return switch (this) {
            case PENDING -> next == APPROVED || next == REJECTED || next == CANCELLED;
            case APPROVED -> next == PROCESSING || next == CANCELLED;
            case PROCESSING -> next == COMPLETED || next == FAILED;
            default -> false;
        };
    }
}
