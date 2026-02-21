package org.odema.posnew.application.service;

import org.odema.posnew.application.dto.request.ShiftReportRequest;
import org.odema.posnew.application.dto.response.ShiftReportDetailResponse;
import org.odema.posnew.application.dto.response.ShiftReportResponse;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ShiftReportService {
    ShiftReportResponse openShift(ShiftReportRequest request, UUID cashierId);

   // ShiftReportResponse closeShift(UUID shiftReportId, BigDecimal closingBalance, BigDecimal actualBalance);

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

    @Transactional
    ShiftReportDetailResponse getShiftDetail(UUID shiftReportId);

    BigDecimal getCashTotal(UUID shiftId);

    BigDecimal getMobileTotal(UUID shiftId);

    BigDecimal getCardTotal(UUID shiftId);

    BigDecimal getCreditTotal(UUID shiftId);

    // Mettre à jour la méthode closeShift pour utiliser les paiements
    @Transactional
    ShiftReportResponse closeShift(UUID shiftReportId, BigDecimal actualBalance, String notes);

    //new method to calculate expected balance based on payments


    List<ShiftReportResponse> getShiftsByCashRegister(UUID cashRegisterId); // AJOUTÉ



    List<ShiftReportResponse> getOpenShiftsByCashRegister(UUID cashRegisterId); // AJOUTÉ


}
