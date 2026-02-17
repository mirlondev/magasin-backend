package org.odema.posnew.repository;

// repository/ReceiptRepository.java

import org.odema.posnew.entity.Receipt;
import org.odema.posnew.entity.enums.ReceiptStatus;
import org.odema.posnew.entity.enums.ReceiptType;
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


//    boolean existsByReceiptNumber(String receiptNumber);
//
//    Optional<Receipt> findByOrder_OrderId(UUID orderId);
//
//    List<Receipt> findByShiftReport_ShiftReportId(UUID shiftReportId);
//
//    List<Receipt> findByReceiptType(ReceiptType receiptType);
//
//    List<Receipt> findByStatus(ReceiptStatus status);





    Optional<Receipt> findByReceiptNumber(String receiptNumber);

    boolean existsByReceiptNumber(String receiptNumber);

    Optional<Receipt> findByOrder_OrderId(UUID orderId);

    List<Receipt> findByShiftReport_ShiftReportId(UUID shiftReportId);

    List<Receipt> findByReceiptType(ReceiptType receiptType);

    List<Receipt> findByStatus(ReceiptStatus status);

    @Query("SELECT r FROM Receipt r " +
            "WHERE r.store.storeId = :storeId " +
            "AND r.receiptDate BETWEEN :startDate AND :endDate " +
            "ORDER BY r.receiptDate DESC")
    List<Receipt> findByStoreAndDateRange(
            @Param("storeId")    UUID storeId,
            @Param("startDate")  LocalDateTime startDate,
            @Param("endDate")    LocalDateTime endDate
    );

    @Query("SELECT r FROM Receipt r " +
            "WHERE r.cashier.userId = :cashierId " +
            "AND r.receiptDate BETWEEN :startDate AND :endDate " +
            "ORDER BY r.receiptDate DESC")
    List<Receipt> findByCashierAndDateRange(
            @Param("cashierId")  UUID cashierId,
            @Param("startDate")  LocalDateTime startDate,
            @Param("endDate")    LocalDateTime endDate
    );

    @Query("SELECT COUNT(r) FROM Receipt r " +
            "WHERE r.store.storeId = :storeId " +
            "AND YEAR(r.receiptDate) = :year " +
            "AND MONTH(r.receiptDate) = :month")
    Long countByStoreAndYearMonth(
            @Param("storeId") UUID storeId,
            @Param("year")    int year,
            @Param("month")   int month
    );
}