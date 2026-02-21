package org.odema.posnew.domain.repository;

import org.odema.posnew.domain.model.Receipt;
import org.odema.posnew.domain.model.enums.ReceiptType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    // ✅ MÉTHODE MANQUANTE - Vérifie l'existence d'un numéro de ticket
    boolean existsByReceiptNumber(String receiptNumber);

    // ✅ MÉTHODE MANQUANTE - Compte les tickets par store et période
    long countByStore_StoreIdAndCreatedAtBetween(UUID storeId, LocalDateTime start, LocalDateTime end);



    @Query("SELECT r FROM Receipt r WHERE r.createdAt BETWEEN :start AND :end")
    List<Receipt> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM Receipt r WHERE r.store.storeId = :storeId " +
            "AND r.createdAt BETWEEN :start AND :end")
    Double getTotalReceiptAmountByStoreAndDateRange(@Param("storeId") UUID storeId,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);
}
