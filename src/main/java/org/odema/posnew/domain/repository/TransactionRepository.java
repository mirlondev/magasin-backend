package org.odema.posnew.domain.repository;

import org.odema.posnew.domain.model.Transaction;
import org.odema.posnew.domain.model.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
