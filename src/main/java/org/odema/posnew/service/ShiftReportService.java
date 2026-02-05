package org.odema.posnew.service;

import org.odema.posnew.dto.request.ShiftReportRequest;
import org.odema.posnew.dto.response.ShiftReportResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ShiftReportService {
    ShiftReportResponse openShift(ShiftReportRequest request, UUID cashierId);

    ShiftReportResponse closeShift(UUID shiftReportId, BigDecimal closingBalance, BigDecimal actualBalance);

    ShiftReportResponse getShiftReportById(UUID shiftReportId);

    ShiftReportResponse getShiftReportByNumber(String shiftNumber);

    List<ShiftReportResponse> getShiftsByCashier(UUID cashierId);

    List<ShiftReportResponse> getShiftsByStore(UUID storeId);

    List<ShiftReportResponse> getShiftsByStatus(String status);

    List<ShiftReportResponse> getShiftsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    ShiftReportResponse getOpenShiftByCashier(UUID cashierId);

    List<ShiftReportResponse> getOpenShiftsByStore(UUID storeId);

    ShiftReportResponse updateShiftReport(UUID shiftReportId, ShiftReportRequest request);

    void suspendShift(UUID shiftReportId, String reason);

    ShiftReportResponse resumeShift(UUID shiftReportId);

    BigDecimal getTotalSalesByStore(UUID storeId);

    BigDecimal getTotalRefundsByStore(UUID storeId);
}
