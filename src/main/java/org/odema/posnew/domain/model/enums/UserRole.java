package org.odema.posnew.domain.enums;

public enum UserRole {
    ADMIN("Administrateur système"),
    STORE_ADMIN("Gérant de magasin"),
    DEPOT_MANAGER("Responsable dépôt"),
    CASHIER("Caissier"),
    EMPLOYEE("Employé"),
    ACCOUNTANT("Comptable");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean canManagePricing() {
        return this == ADMIN || this == STORE_ADMIN;
    }

    public boolean canProcessCredit() {
        return this == ADMIN || this == STORE_ADMIN || this == ACCOUNTANT;
    }

    public boolean canApproveRefund() {
        return this == ADMIN || this == STORE_ADMIN;
    }
}
