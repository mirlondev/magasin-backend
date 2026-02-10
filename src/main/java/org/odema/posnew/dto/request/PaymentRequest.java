package org.odema.posnew.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.odema.posnew.entity.enums.PaymentMethod;

import java.math.BigDecimal;

/**
 * DTO pour créer un paiement
 */
public record PaymentRequest(
        @NotNull(message = "La méthode de paiement est obligatoire")
        PaymentMethod method,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
        BigDecimal amount,

        String notes
) {
}