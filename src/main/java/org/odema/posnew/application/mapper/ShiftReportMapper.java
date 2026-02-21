package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.response.ShiftReportDetailResponse;
import org.odema.posnew.application.dto.response.ShiftReportResponse;
import org.odema.posnew.domain.model.ShiftReport;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ShiftReportMapper {

    public ShiftReportResponse toResponse(ShiftReport shift) {
        if (shift == null) return null;

        return new ShiftReportResponse(
                shift.getShiftReportId(),
                shift.getShiftNumber(),
                shift.getCashier() != null ? shift.getCashier().getUserId() : null,
                shift.getCashier() != null ? shift.getCashier().getUsername() : null,
                shift.getStore() != null ? shift.getStore().getStoreId() : null,
                shift.getStore() != null ? shift.getStore().getName() : null,
                shift.getCashRegister() != null ? shift.getCashRegister().getCashRegisterId() : null,
                shift.getCashRegister() != null ? shift.getCashRegister().getRegisterNumber() : null,
                shift.getCashRegister() != null ? shift.getCashRegister().getName() : null,
                shift.getOpeningTime(),
                shift.getClosingTime(),
                shift.getOpeningBalance(),
                shift.getClosingBalance(),
                shift.getExpectedBalance(),
                shift.getActualBalance(),
                shift.getDiscrepancy(),
                shift.getTotalTransactions(),
                shift.getTotalSales(),
                shift.getTotalRefunds(),
                shift.getNetSales(),
                shift.getNotes(),
                shift.getStatus(),
                shift.getCreatedAt(),
                shift.getUpdatedAt()
        );
    }

    public List<ShiftReportResponse> toResponseList(List<ShiftReport> shifts) {
        if (shifts == null) return List.of();
        return shifts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ShiftReportDetailResponse toDetailResponse(ShiftReport shift) {
        if (shift == null) return null;

        return ShiftReportDetailResponse.builder()
                .shiftReportId(shift.getShiftReportId())
                .shiftNumber(shift.getShiftNumber())
                .cashierId(shift.getCashier() != null ? shift.getCashier().getUserId() : null)
                .cashierName(shift.getCashier() != null ? shift.getCashier().getUsername() : null)
                .storeId(shift.getStore() != null ? shift.getStore().getStoreId() : null)
                .storeName(shift.getStore() != null ? shift.getStore().getName() : null)
                .cashRegisterId(shift.getCashRegister() != null ? shift.getCashRegister().getCashRegisterId() : null)
                .cashRegisterNumber(shift.getCashRegister() != null ? shift.getCashRegister().getRegisterNumber() : null)
                .cashRegisterName(shift.getCashRegister() != null ? shift.getCashRegister().getName() : null)
                .startTime(shift.getOpeningTime())
                .endTime(shift.getClosingTime())
                .openingBalance(shift.getOpeningBalance())
                .closingBalance(shift.getClosingBalance())
                .expectedBalance(shift.getExpectedBalance())
                .actualBalance(shift.getActualBalance())
                .discrepancy(shift.getDiscrepancy())
                .totalTransactions(shift.getTotalTransactions())
                .totalSales(shift.getTotalSales())
                .totalRefunds(shift.getTotalRefunds())
                .netSales(shift.getNetSales())
                .cashTotal(shift.getCashSales() != null ? shift.getCashSales() : BigDecimal.ZERO)
                .mobileTotal(shift.getMobileMoneySales() != null ? shift.getMobileMoneySales() : BigDecimal.ZERO)
                .cardTotal(shift.getCardSales() != null ? shift.getCardSales() : BigDecimal.ZERO)
                .creditTotal(shift.getCreditSales() != null ? shift.getCreditSales() : BigDecimal.ZERO)
                .otherPayments(new HashMap<>())
                .notes(shift.getNotes())
                .status(shift.getStatus())
                .createdAt(shift.getCreatedAt())
                .updatedAt(shift.getUpdatedAt())
                .build();
    }
}