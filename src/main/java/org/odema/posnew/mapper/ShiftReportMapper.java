package org.odema.posnew.mapper;

import org.odema.posnew.entity.ShiftReport;
import org.odema.posnew.dto.response.ShiftReportResponse;
import org.odema.posnew.dto.response.ShiftReportDetailResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;

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

                // AJOUTÉ - infos caisse
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

    public ShiftReportDetailResponse toDetailResponse(ShiftReport shift) {
        if (shift == null) return null;

        return ShiftReportDetailResponse.builder()
                .shiftReportId(shift.getShiftReportId())
                .shiftNumber(shift.getShiftNumber())
                .cashierId(shift.getCashier() != null ? shift.getCashier().getUserId() : null)
                .cashierName(shift.getCashier() != null ? shift.getCashier().getUsername() : null)
                .storeId(shift.getStore() != null ? shift.getStore().getStoreId() : null)
                .storeName(shift.getStore() != null ? shift.getStore().getName() : null)
                // AJOUTÉ - infos caisse
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
                .cashTotal(BigDecimal.ZERO) // Sera calculé par le service
                .mobileTotal(BigDecimal.ZERO)
                .cardTotal(BigDecimal.ZERO)
                .creditTotal(BigDecimal.ZERO)
                .otherPayments(new HashMap<>())
                .notes(shift.getNotes())
                .status(shift.getStatus())
                .createdAt(shift.getCreatedAt())
                .updatedAt(shift.getUpdatedAt())
                .build();
    }
}