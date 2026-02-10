package org.odema.posnew.dto.response;

import lombok.Builder;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record ShiftReportDetailResponse(
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
        BigDecimal cashTotal,
        BigDecimal mobileTotal,
        BigDecimal cardTotal,
        BigDecimal creditTotal,
        Map<PaymentMethod, BigDecimal> otherPayments,
        String notes,
        ShiftStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
