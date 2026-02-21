package org.odema.posnew.application.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrder_OrderId(UUID orderId);

    List<Payment> findByShiftReport_ShiftReportId(UUID shiftReportId);

    List<Payment> findByCashier_UserId(UUID cashierId);

    List<Payment> findByMethod(PaymentMethod method);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.orderId = :orderId " +
            "AND p.status = 'PAID' AND p.method != 'CREDIT'")
    BigDecimal getTotalPaidByOrder(@Param("orderId") UUID orderId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order.orderId = :orderId " +
            "AND p.method = 'CREDIT' AND p.status = 'CREDIT'")
    BigDecimal getTotalCreditByOrder(@Param("orderId") UUID orderId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.shiftReport.shiftReportId = :shiftId " +
            "AND p.method = :method AND p.status = 'PAID'")
    BigDecimal sumByMethodAndShift(@Param("method") PaymentMethod method, @Param("shiftId") UUID shiftId);

    Long countByShiftReport_ShiftReportIdAndStatus(UUID shiftId, PaymentStatus status);
}
