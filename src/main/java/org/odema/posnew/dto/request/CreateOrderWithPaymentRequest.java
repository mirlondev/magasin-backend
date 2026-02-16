package org.odema.posnew.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Combined request for creating order with initial payment
 */
public record CreateOrderWithPaymentRequest(
        @NotNull @Valid OrderRequest orderRequest,
        PaymentRequest paymentRequest  // Optional - can be null for unpaid orders
) {}