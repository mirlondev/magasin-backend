package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request for order item - prices are calculated from product, not sent by client!
 */
public record OrderItemRequest(
        @NotNull(message = "Le produit est obligatoire")
        UUID productId,

        @NotNull(message = "La quantité est obligatoire")
        @Positive(message = "La quantité doit être supérieure à 0")
        Integer quantity,

        BigDecimal discountPercentage,  // Optional: % discount on this item

        String notes
) {
    public OrderItemRequest {
        if (discountPercentage == null) discountPercentage = BigDecimal.ZERO;
    }
}