package org.odema.posnew.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemRequest(
        @NotNull(message = "Le produit est obligatoire")
        UUID productId,

        @NotNull(message = "La quantité est obligatoire")
        @Min(value = 1, message = "La quantité doit être au moins 1")
        Integer quantity,

        BigDecimal discountPercentage,

        String notes
) {
    public BigDecimal discountAmount() {
        if (discountPercentage == null) {
            return BigDecimal.ZERO;
        }
        return discountPercentage.multiply(BigDecimal.valueOf(quantity)).divide(BigDecimal.valueOf(100));
    }
}
