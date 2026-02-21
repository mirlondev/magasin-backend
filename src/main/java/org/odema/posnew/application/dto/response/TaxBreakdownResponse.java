package org.odema.posnew.application.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record TaxBreakdownResponse(
        UUID orderItemId,
        String productName,
        BigDecimal baseAmount,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal totalWithTax
) {
}
