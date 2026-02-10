package org.odema.posnew.dto.response;

import org.odema.posnew.entity.PaymentMethod;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull PaymentMethod method,
        @NotNull BigDecimal amount,
        String notes
) {
}
