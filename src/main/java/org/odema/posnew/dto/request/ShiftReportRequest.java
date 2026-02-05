package org.odema.posnew.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record ShiftReportRequest(
        UUID storeId,

        BigDecimal openingBalance,

        String notes
) {
}
