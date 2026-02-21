package org.odema.posnew.application.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record ShiftReportRequest(
        UUID storeId,
        UUID cashRegisterId,
        BigDecimal openingBalance,
        String notes
) {
}
