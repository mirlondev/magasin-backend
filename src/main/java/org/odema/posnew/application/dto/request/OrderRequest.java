package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.odema.posnew.domain.enums_old.OrderType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Request to create an order - NO payment info here!
 * Payments are added separately via PaymentRequest
 */
public record OrderRequest(
        UUID customerId,

        @NotNull(message = "Le store est obligatoire")
        UUID storeId,

        @NotEmpty(message = "La commande doit contenir au moins un article")
        List<OrderItemRequest> items,

        BigDecimal discountAmount,

        BigDecimal taxRate,

        Boolean isTaxable,

        String notes,

        OrderType orderType
) {
        // Compact constructor with defaults
        public OrderRequest {
                if (discountAmount == null) discountAmount = BigDecimal.ZERO;
                if (taxRate == null) taxRate = BigDecimal.ZERO;
                if (isTaxable == null) isTaxable = true;
                if (orderType == null) orderType = OrderType.POS_SALE;
        }
}