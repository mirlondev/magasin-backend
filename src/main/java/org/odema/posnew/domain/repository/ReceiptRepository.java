package org.odema.posnew.application.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    Optional<Receipt> findByOrder_OrderId(UUID orderId);

    List<Receipt> findByShiftReport_ShiftReportId(UUID shiftReportId);

    List<Receipt> findByCashier_UserId(UUID cashierId);

    List<Receipt> findByStore_StoreId(UUID storeId);

    List<Receipt> findByReceiptType(ReceiptType type);

    @Query("SELECT r FROM Receipt r WHERE r.store.storeId = :storeId " +
            "AND r.receiptDate BETWEEN :start AND :end")
    List<Receipt> findByStoreAndDateRange(@Param("storeId") UUID storeId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Receipt r WHERE r.cashier.userId = :cashierId " +
            "AND r.receiptDate BETWEEN :start AND :end")
    List<Receipt> findByCashierAndDateRange(@Param("cashierId") UUID cashierId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);
}
