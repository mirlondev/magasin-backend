package org.odema.posnew.application.dto.request;

import org.odema.posnew.domain.model.enums.PaymentMethod;

import java.math.BigDecimal;

public record PaymentRequest(
        PaymentMethod method,
        BigDecimal amount,
        String notes
) {
}
