package org.odema.posnew.repository;

import org.odema.posnew.entity.ShiftReport;
import org.odema.posnew.entity.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftReportRepository extends JpaRepository<ShiftReport, UUID> {

    Optional<ShiftReport> findByShiftNumber(String shiftNumber);

    List<ShiftReport> findByCashier_UserId(UUID cashierId);

    List<ShiftReport> findByStore_StoreId(UUID storeId);

    List<ShiftReport> findByStatus(ShiftStatus status);

    @Query("SELECT sr FROM ShiftReport sr WHERE sr.cashier.userId = :cashierId AND sr.status = 'OPEN'")
    Optional<ShiftReport> findOpenShiftByCashier(@Param("cashierId") UUID cashierId);

    @Query("SELECT sr FROM ShiftReport sr WHERE sr.store.storeId = :storeId AND sr.status = 'OPEN'")
    List<ShiftReport> findOpenShiftsByStore(@Param("storeId") UUID storeId);

    @Query("SELECT sr FROM ShiftReport sr WHERE sr.startTime BETWEEN :startDate AND :endDate")
    List<ShiftReport> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sr FROM ShiftReport sr WHERE sr.store.storeId = :storeId AND sr.startTime BETWEEN :startDate AND :endDate")
    List<ShiftReport> findByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(sr.totalSales), 0) FROM ShiftReport sr WHERE sr.store.storeId = :storeId AND sr.status = 'CLOSED'")
    Double getTotalSalesByStore(@Param("storeId") UUID storeId);

    @Query("SELECT COALESCE(SUM(sr.totalRefunds), 0) FROM ShiftReport sr WHERE sr.store.storeId = :storeId AND sr.status = 'CLOSED'")
    Double getTotalRefundsByStore(@Param("storeId") UUID storeId);
}
