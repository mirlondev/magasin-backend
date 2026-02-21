package org.odema.posnew.domain.service;

import org.odema.posnew.application.dto.response.TaxBreakdownResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TaxService {

    BigDecimal calculateTax(BigDecimal baseAmount, BigDecimal taxRate);

    BigDecimal calculateTaxForOrder(UUID orderId);

    List<TaxBreakdownResponse> getTaxBreakdown(UUID orderId);

    void validateTaxConfiguration(UUID storeId);
}
