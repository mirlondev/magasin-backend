package org.odema.posnew.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record ShiftReportRequest(
        UUID storeId,
        UUID cashRegisterId,  // AJOUTÉ - numéro de caisse
        BigDecimal openingBalance,
        String notes
) {
}