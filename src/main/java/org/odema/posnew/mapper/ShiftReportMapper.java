package org.odema.posnew.mapper;

import org.odema.posnew.entity.ShiftReport;
import org.odema.posnew.dto.response.ShiftReportResponse;
import org.springframework.stereotype.Component;

@Component
public class ShiftReportMapper {

    public ShiftReportResponse toResponse(ShiftReport shift) {
        if (shift == null) return null;

        return new ShiftReportResponse(
                shift.getShiftReportId(),
                shift.getShiftNumber(),

                shift.getCashier() != null ? shift.getCashier().getUserId() : null,
                shift.getCashier() != null ? shift.getCashier().getUsername() : null,

                shift.getStore() != null ? java.util.UUID.fromString(String.valueOf(shift.getStore().getStoreId())) : null,
                shift.getStore() != null ? shift.getStore().getName() : null,

                shift.getStartTime(),
                shift.getEndTime(),

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
}
