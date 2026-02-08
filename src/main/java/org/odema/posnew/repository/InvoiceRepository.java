package org.odema.posnew.repository;

import org.springframework.data.jpa.repository.Query;
import org.odema.posnew.entity.Invoice;
import org.odema.posnew.entity.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByOrder_OrderId(UUID orderId);

    List<Invoice> findByCustomer_CustomerId(UUID customerId);

    List<Invoice> findByStore_StoreId(String storeId);

    List<Invoice> findByStatus(InvoiceStatus status);

    @Query("SELECT i FROM Invoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate")
    List<Invoice> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT i FROM Invoice i WHERE i.paymentDueDate < :currentDate AND i.status = 'ISSUED'")
    List<Invoice> findOverdueInvoices(@Param("currentDate") LocalDateTime currentDate);

    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.status = 'PAID' AND i.invoiceDate BETWEEN :startDate AND :endDate")
    Double getTotalPaidInvoicesAmount(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.status = 'OVERDUE'")
    Long countOverdueInvoices();

    @Query("SELECT COALESCE(SUM(i.amountDue), 0) FROM Invoice i WHERE i.status = 'ISSUED'")
    Double getTotalOutstandingAmount();
}
