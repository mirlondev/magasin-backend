package org.odema.posnew.application.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Optional<Refund> findByRefundNumber(String refundNumber);

    List<Refund> findByOrder_OrderId(UUID orderId);

    List<Refund> findByStore_StoreId(UUID storeId);

    List<Refund> findByStatus(RefundStatus status);

    List<Refund> findByShiftReport_ShiftReportId(UUID shiftReportId);

    @Query("SELECT r FROM Refund r WHERE r.store.storeId = :storeId " +
            "AND r.createdAt BETWEEN :start AND :end")
    List<Refund> findByStoreAndDateRange(@Param("storeId") UUID storeId,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(r.totalRefundAmount), 0) FROM Refund r " +
            "WHERE r.store.storeId = :storeId AND r.status = 'COMPLETED' " +
            "AND r.createdAt BETWEEN :start AND :end")
    BigDecimal sumCompletedRefundsByStoreAndDateRange(@Param("storeId") UUID storeId,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(ri.quantity), 0) FROM RefundItem ri " +
            "WHERE ri.product.productId = :productId AND ri.refund.status = 'COMPLETED'")
    Integer sumQuantityByProductAndCompleted(@Param("productId") UUID productId);
}
