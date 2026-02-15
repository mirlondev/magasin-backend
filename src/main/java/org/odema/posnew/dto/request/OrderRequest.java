package org.odema.posnew.dto.request;

import jakarta.validation.constraints.NotNull;
import org.odema.posnew.entity.enums.OrderType;
import org.odema.posnew.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderRequest(
        UUID customerId,

        @NotNull(message = "Le store est obligatoire")
        UUID storeId,

        @NotNull(message = "Les articles sont obligatoires")
        List<OrderItemRequest> items,

        BigDecimal discountAmount,

        PaymentMethod paymentMethod,

        BigDecimal amountPaid,

        BigDecimal taxRate,

        Boolean isTaxable,

        String notes,
        OrderType   orderType
) {
}
