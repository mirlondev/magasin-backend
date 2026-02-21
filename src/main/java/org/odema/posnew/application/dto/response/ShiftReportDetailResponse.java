package org.odema.posnew.application.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.odema.posnew.domain.model.enums.PaymentMethod;
import org.odema.posnew.domain.model.enums.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
@Getter @Setter
@Builder
public class  ShiftReportDetailResponse{
        UUID shiftReportId;
        String shiftNumber;

        UUID cashierId;
        String cashierName;

        UUID storeId;
        String storeName;

        UUID cashRegisterId;
        String cashRegisterNumber;
        String cashRegisterName;

        LocalDateTime startTime;
        LocalDateTime endTime;

        BigDecimal openingBalance;
        BigDecimal closingBalance;
        BigDecimal expectedBalance;
        BigDecimal actualBalance;
        BigDecimal discrepancy;

        Integer totalTransactions;
        BigDecimal totalSales;
        BigDecimal totalRefunds;
        BigDecimal netSales;

        BigDecimal cashTotal;
        BigDecimal mobileTotal;
        BigDecimal cardTotal;
        BigDecimal creditTotal;
        Map<PaymentMethod, BigDecimal> otherPayments;

        String notes;
        ShiftStatus status;

        LocalDateTime createdAt;
        LocalDateTime updatedAt;

}
