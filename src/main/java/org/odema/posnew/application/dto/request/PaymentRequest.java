package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.odema.posnew.domain.enums_old.PaymentMethod;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull(message = "Le mode de paiement est obligatoire")
        PaymentMethod method,

        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit être supérieur à 0")
        BigDecimal amount,

        String notes
) {
        // Compact constructor with validation
        public PaymentRequest {
                if (method == null) {
                        throw new IllegalArgumentException("Le mode de paiement ne peut pas être null");
                }
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Le montant doit être supérieur à 0");
                }
        }
}