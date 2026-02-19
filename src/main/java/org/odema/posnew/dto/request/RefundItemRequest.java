package org.odema.posnew.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO pour un article à rembourser.
 */
public record RefundItemRequest(
        UUID orderItemId,
        Integer quantity,
        BigDecimal restockingFee,
        String reason,
        Boolean isExchange,
        UUID exchangeProductId,
        Integer exchangeQuantity
) {
    /**
     * Constructeur compact avec validation
     */
    public RefundItemRequest {
        if (orderItemId == null) {
            throw new IllegalArgumentException("L'ID de l'article de commande est obligatoire");
        }
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("La quantité doit être supérieure à 0");
        }
        if (restockingFee == null) {
            restockingFee = BigDecimal.ZERO;
        }
        if (isExchange == null) {
            isExchange = false;
        }
    }
}
