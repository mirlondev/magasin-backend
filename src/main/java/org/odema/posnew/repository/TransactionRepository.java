package org.odema.posnew.repository;

import org.odema.posnew.entity.Transaction;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.TransactionType;
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
 * Repository pour les transactions financières.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionNumber(String transactionNumber);

    List<Transaction> findByTransactionType(TransactionType type);

    List<Transaction> findByCashier_UserId(UUID cashierId);

    List<Transaction> findByStore_StoreId(UUID storeId);

    List<Transaction> findByShiftReport_ShiftReportId(UUID shiftReportId);

    List<Transaction> findByOrder_OrderId(UUID orderId);

    List<Transaction> findByPaymentMethod(PaymentMethod method);

    @Query("SELECT t FROM Transaction t WHERE t.store.storeId = :storeId AND t.transactionDate BETWEEN :startDate AND :endDate AND t.isVoided = false ORDER BY t.transactionDate DESC")
    List<Transaction> findByStoreAndDateRange(@Param("storeId") UUID storeId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE t.shiftReport.shiftReportId = :shiftId AND t.isVoided = false ORDER BY t.transactionDate")
    List<Transaction> findActiveByShiftReport(@Param("shiftId") UUID shiftId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.shiftReport.shiftReportId = :shiftId AND t.transactionType = :type AND t.isVoided = false")
    BigDecimal sumByShiftAndType(@Param("shiftId") UUID shiftId, @Param("type") TransactionType type);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.shiftReport.shiftReportId = :shiftId AND t.paymentMethod = :method AND t.isVoided = false")
    BigDecimal sumByShiftAndPaymentMethod(@Param("shiftId") UUID shiftId, @Param("method") PaymentMethod method);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.shiftReport.shiftReportId = :shiftId AND t.isVoided = false")
    Long countByShiftReport(@Param("shiftId") UUID shiftId);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.store.storeId = :storeId AND t.transactionType IN :types AND t.isVoided = false AND t.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal sumByStoreAndTypesAndDateRange(@Param("storeId") UUID storeId,
                                              @Param("types") List<TransactionType> types,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t.transactionType, SUM(t.amount) FROM Transaction t WHERE t.shiftReport.shiftReportId = :shiftId AND t.isVoided = false GROUP BY t.transactionType")
    List<Object[]> sumGroupByTypeForShift(@Param("shiftId") UUID shiftId);

    boolean existsByTransactionNumber(String transactionNumber);

    List<Transaction> findByIsReconciledFalseAndIsVoidedFalse();

    @Query("SELECT t FROM Transaction t WHERE t.isVoided = false AND t.transactionDate BETWEEN :startDate AND :endDate ORDER BY t.transactionDate DESC")
    List<Transaction> findActiveByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
