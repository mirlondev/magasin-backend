package org.odema.posnew.repository;

import org.odema.posnew.entity.Refund;
import org.odema.posnew.entity.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Optional<Refund> findByRefundNumber(String refundNumber);

    List<Refund> findByOrder_OrderId(UUID orderId);

    List<Refund> findByStore_StoreId(UUID storeId);

    List<Refund> findByCashier_UserId(UUID cashierId);

    List<Refund> findByStatus(RefundStatus status);

    @Query("SELECT r FROM Refund r WHERE r.shiftReport.shiftReportId = :shiftReportId")
    List<Refund> findByShiftReport(@Param("shiftReportId") UUID shiftReportId);

    @Query("SELECT SUM(r.refundAmount) FROM Refund r WHERE r.order.orderId = :orderId AND r.status = 'COMPLETED'")
    BigDecimal getTotalRefundedAmountByOrder(@Param("orderId") UUID orderId);

    @Query("SELECT COUNT(r) FROM Refund r WHERE r.store.storeId = :storeId AND r.status = 'COMPLETED'")
    Integer countCompletedRefundsByStore(@Param("storeId") UUID storeId);

    @Query("SELECT SUM(r.refundAmount) FROM Refund r WHERE r.store.storeId = :storeId AND r.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRefundsByStoreAndDateRange(
            @Param("storeId") UUID storeId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT r FROM Refund r WHERE r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    List<Refund> findRefundsByDateRange(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate);
}
