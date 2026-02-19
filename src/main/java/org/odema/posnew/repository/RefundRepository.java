package org.odema.posnew.repository;

import org.odema.posnew.entity.Refund;
import org.odema.posnew.entity.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour les remboursements.
 */
@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Optional<Refund> findByRefundNumber(String refundNumber);

    List<Refund> findByOrder_OrderId(UUID orderId);

    List<Refund> findByStatus(RefundStatus status);

    List<Refund> findByCashier_UserId(UUID cashierId);

    List<Refund> findByStore_StoreId(UUID storeId);

    List<Refund> findByShiftReport_ShiftReportId(UUID shiftReportId);

    @Query("SELECT r FROM Refund r WHERE r.store.storeId = :storeId AND r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    List<Refund> findByStoreAndDateRange(@Param("storeId") UUID storeId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT r FROM Refund r WHERE r.cashier.userId = :cashierId AND r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    List<Refund> findByCashierAndDateRange(@Param("cashierId") UUID cashierId,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(r.refundAmount) FROM Refund r WHERE r.store.storeId = :storeId AND r.status = 'COMPLETED' AND r.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumCompletedRefundsByStoreAndDateRange(@Param("storeId") UUID storeId,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(r) FROM Refund r WHERE r.shiftReport.shiftReportId = :shiftId AND r.status = 'COMPLETED'")
    Long countCompletedByShiftReport(@Param("shiftId") UUID shiftId);

    @Query("SELECT SUM(r.refundAmount) FROM Refund r WHERE r.shiftReport.shiftReportId = :shiftId AND r.status = 'COMPLETED'")
    BigDecimal sumAmountByShiftReport(@Param("shiftId") UUID shiftId);

    boolean existsByRefundNumber(String refundNumber);

    List<Refund> findByStatusIn(List<RefundStatus> statuses);

    @Query("SELECT r FROM Refund r WHERE r.status = 'PENDING' AND r.createdAt < :date")
    List<Refund> findPendingRefundsOlderThan(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(r) FROM Refund r WHERE r.store.storeId = :storeId AND r.status = 'COMPLETED'")
    Integer countCompletedRefundsByStore(UUID storeId);

    @Query("SELECT SUM(r.refundAmount) FROM Refund r WHERE r.store.storeId = :storeId AND r.status = 'COMPLETED' AND r.createdAt BETWEEN :startDateTime AND :endDateTime")
    BigDecimal getTotalRefundsByStoreAndDateRange(UUID storeId, LocalDateTime startDateTime, LocalDateTime endDateTime);
}
