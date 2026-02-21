package org.odema.posnew.application.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftReportRepository extends JpaRepository<ShiftReport, UUID> {

    Optional<ShiftReport> findByShiftNumber(String shiftNumber);

    Optional<ShiftReport> findByCashier_UserIdAndStatus(UUID cashierId, ShiftStatus status);

    @Query("SELECT s FROM ShiftReport s WHERE s.cashier.userId = :cashierId AND s.status = 'OPEN'")
    Optional<ShiftReport> findOpenShiftByCashier(@Param("cashierId") UUID cashierId);

    @Query("SELECT s FROM ShiftReport s WHERE s.cashRegister.cashRegisterId = :registerId AND s.status = 'OPEN'")
    Optional<ShiftReport> findOpenShiftByCashRegister(@Param("registerId") UUID registerId);

    List<ShiftReport> findByCashier_UserId(UUID cashierId);

    List<ShiftReport> findByStore_StoreId(UUID storeId);

    List<ShiftReport> findByCashRegister_CashRegisterId(UUID cashRegisterId);

    List<ShiftReport> findByStatus(ShiftStatus status);

    @Query("SELECT s FROM ShiftReport s WHERE s.store.storeId = :storeId AND s.status = 'OPEN'")
    List<ShiftReport> findOpenShiftsByStore(@Param("storeId") UUID storeId);

    @Query("SELECT s FROM ShiftReport s WHERE s.openingTime BETWEEN :start AND :end")
    List<ShiftReport> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.totalSales), 0) FROM ShiftReport s WHERE s.store.storeId = :storeId")
    Double getTotalSalesByStore(@Param("storeId") UUID storeId);

    @Query("SELECT COALESCE(SUM(s.totalRefunds), 0) FROM ShiftReport s WHERE s.store.storeId = :storeId")
    Double getTotalRefundsByStore(@Param("storeId") UUID storeId);
}
