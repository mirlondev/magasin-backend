package org.odema.posnew.repository;

import org.odema.posnew.entity.Payment;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Trouve tous les paiements d'une commande
     */
    List<Payment> findByOrder_OrderId(UUID orderId);

    /**
     * Trouve tous les paiements d'un caissier
     */
    List<Payment> findByCashier_UserId(UUID cashierId);

    /**
     * Trouve tous les paiements d'un shift
     */
    List<Payment> findByShiftReport_ShiftReportId(UUID shiftReportId);

    /**
     * Trouve les paiements par méthode
     */
    List<Payment> findByMethod(PaymentMethod method);

    /**
     * Trouve les paiements par statut
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Somme des paiements pour un shift par méthode
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.shiftReport.shiftReportId = :shiftId " +
            "AND p.method = :method " +
            "AND p.status = org.odema.posnew.entity.enums.PaymentStatus.PAID " +
            "AND p.isActive = true")
    BigDecimal sumByMethodAndShift(@Param("method") PaymentMethod method,
                                   @Param("shiftId") UUID shiftId);

    /**
     * Somme des paiements pour une commande par statut
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.order.orderId = :orderId " +
            "AND p.status = :status " +
            "AND p.isActive = true")
    BigDecimal sumByOrderAndStatus(@Param("orderId") UUID orderId,
                                   @Param("status") PaymentStatus status);

    /**
     * Total payé pour une commande (exclut les crédits)
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.order.orderId = :orderId " +
            "AND p.status = org.odema.posnew.entity.enums.PaymentStatus.PAID " +
            "AND p.method != org.odema.posnew.entity.enums.PaymentMethod.CREDIT " +
            "AND p.isActive = true")
    BigDecimal getTotalPaidByOrder(@Param("orderId") UUID orderId);

    /**
     * Total en crédit pour une commande
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.order.orderId = :orderId " +
            "AND p.method = org.odema.posnew.entity.enums.PaymentMethod.CREDIT " +
            "AND p.status = org.odema.posnew.entity.enums.PaymentStatus.CREDIT " +
            "AND p.isActive = true")
    BigDecimal getTotalCreditByOrder(@Param("orderId") UUID orderId);

    /**
     * Paiements d'un shift par plage de dates
     */
    @Query("SELECT p FROM Payment p " +
            "WHERE p.shiftReport.shiftReportId = :shiftId " +
            "AND p.createdAt BETWEEN :startDate AND :endDate " +
            "AND p.isActive = true")
    List<Payment> findByShiftAndDateRange(@Param("shiftId") UUID shiftId,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Paiements actifs d'une commande
     */
    @Query("SELECT p FROM Payment p " +
            "WHERE p.order.orderId = :orderId " +
            "AND p.isActive = true " +
            "ORDER BY p.createdAt DESC")
    List<Payment> findActiveByOrder(@Param("orderId") UUID orderId);

    /**
     * Compte les paiements d'un shift par méthode
     */
    @Query("SELECT COUNT(p) FROM Payment p " +
            "WHERE p.shiftReport.shiftReportId = :shiftId " +
            "AND p.method = :method " +
            "AND p.status = org.odema.posnew.entity.enums.PaymentStatus.PAID " +
            "AND p.isActive = true")
    Long countByShiftReport_ShiftReportIdAndStatus(@Param("shiftId") UUID shiftId,
                               @Param("method") PaymentStatus method);

    /**
     * Total des paiements cash pour un store sur une période
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.order.store.storeId = :storeId " +
            "AND p.method = org.odema.posnew.entity.enums.PaymentMethod.CASH " +
            "AND p.createdAt BETWEEN :startDate AND :endDate " +
            "AND p.status = org.odema.posnew.entity.enums.PaymentStatus.PAID " +
            "AND p.isActive = true")
    BigDecimal getTotalCashByStoreAndDateRange(@Param("storeId") UUID storeId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Total des paiements mobile pour un store sur une période
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.order.store.storeId = :storeId " +
            "AND p.method = org.odema.posnew.entity.enums.PaymentMethod.MOBILE_MONEY " +
            "AND p.createdAt BETWEEN :startDate AND :endDate " +
            "AND p.status = org.odema.posnew.entity.enums.PaymentStatus.PAID " +
            "AND p.isActive = true")
    BigDecimal getTotalMobileByStoreAndDateRange(@Param("storeId") UUID storeId,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

}