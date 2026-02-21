package org.odema.posnew.domain.repository;

import org.odema.posnew.domain.model.Invoice;
import org.odema.posnew.domain.model.enums.InvoiceStatus;
import org.odema.posnew.domain.model.enums.InvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    // ✅ MÉTHODE MANQUANTE - Vérifie l'existence d'un numéro de facture
    boolean existsByInvoiceNumber(String invoiceNumber);

    // ✅ MÉTHODE MANQUANTE - Compte les factures par type et période
    long countByInvoiceTypeAndCreatedAtBetween(InvoiceType type, LocalDateTime start, LocalDateTime end);

    Optional<Invoice> findByOrder_OrderId(UUID orderId);

    Optional<Invoice> findByOrder_OrderIdAndInvoiceType(UUID orderId, InvoiceType type);

    List<Invoice> findByCustomer_CustomerId(UUID customerId);

    List<Invoice> findByStore_StoreId(UUID storeId);

    List<Invoice> findByStatus(InvoiceStatus status);

    List<Invoice> findByInvoiceType(InvoiceType type);

    @Query("SELECT i FROM Invoice i WHERE i.createdAt BETWEEN :start AND :end")
    List<Invoice> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT i FROM Invoice i WHERE i.paymentDueDate < :now AND i.status NOT IN ('PAID', 'CANCELLED')")
    List<Invoice> findOverdueInvoices(@Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(i.amountDue), 0) FROM Invoice i WHERE i.status = 'ISSUED'")
    Double getTotalOutstandingAmount();

    boolean existsByOrder_OrderId(UUID orderId);
}