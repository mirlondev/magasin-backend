package org.odema.posnew.dto.response;

import org.odema.posnew.entity.enums.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShiftReportResponse(
        UUID shiftReportId,
        String shiftNumber,

        UUID cashierId,
        String cashierName,

        UUID storeId,
        String storeName,

        LocalDateTime startTime,
        LocalDateTime endTime,

        BigDecimal openingBalance,
        BigDecimal closingBalance,
        BigDecimal expectedBalance,
        BigDecimal actualBalance,
        BigDecimal discrepancy,

        Integer totalTransactions,
        BigDecimal totalSales,
        BigDecimal totalRefunds,
        BigDecimal netSales,

        String notes,
        ShiftStatus status,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
