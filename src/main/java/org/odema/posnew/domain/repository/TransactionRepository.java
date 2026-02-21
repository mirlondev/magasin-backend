package org.odema.posnew.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByShiftReport_ShiftReportId(UUID shiftReportId);

    List<Transaction> findByCashier_UserId(UUID cashierId);

    List<Transaction> findByStore_StoreId(UUID storeId);

    List<Transaction> findByTransactionType(TransactionType type);

    List<Transaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);

    List<Transaction> findByIsReconciledFalse();
}
