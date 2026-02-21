package org.odema.posnew.domain.model.enums;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum LoyaltyTier {
    BRONZE("Bronze", BigDecimal.ZERO, BigDecimal.valueOf(500), 1.0),
    SILVER("Argent", BigDecimal.valueOf(0.05), BigDecimal.valueOf(2000), 1.5),
    GOLD("Or", BigDecimal.valueOf(0.10), BigDecimal.valueOf(5000), 2.0),
    PLATINUM("Platine", BigDecimal.valueOf(0.15), BigDecimal.valueOf(10000), 3.0);

    private final String label;
    private final BigDecimal discountRate;
    private final BigDecimal threshold;
    private final double pointMultiplier;

    LoyaltyTier(String label, BigDecimal discountRate, BigDecimal threshold, double pointMultiplier) {
        this.label = label;
        this.discountRate = discountRate;
        this.threshold = threshold;
        this.pointMultiplier = pointMultiplier;
    }

    public static LoyaltyTier fromAmount(BigDecimal amount) {
        for (int i = values().length - 1; i >= 0; i--) {
            if (amount.compareTo(values()[i].threshold) >= 0) {
                return values()[i];
            }
        }
        return BRONZE;
    }

    public int calculatePoints(BigDecimal amount) {
        return (int) (amount.doubleValue() * pointMultiplier);
    }
}
