package org.odema.posnew.application.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByCustomer_CustomerId(UUID customerId);

    List<Order> findByCashier_UserId(UUID cashierId);

    Page<Order> findByCashier_UserId(UUID cashierId, Pageable pageable);

    List<Order> findByStore_StoreId(UUID storeId);

    Page<Order> findByStore_StoreId(UUID storeId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.payments WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithPayments(@Param("orderId") UUID orderId);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") UUID orderId);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    List<Order> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT o FROM Order o WHERE o.status = 'COMPLETED' ORDER BY o.completedAt DESC")
    List<Order> findRecentCompletedOrders(Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.cashier.userId = :cashierId AND o.shiftReport.shiftReportId = :shiftId")
    List<Order> findCashierOrdersByShift(@Param("cashierId") UUID cashierId, @Param("shiftId") UUID shiftId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.store.storeId = :storeId " +
            "AND o.status = 'COMPLETED' AND o.completedAt BETWEEN :start AND :end")
    BigDecimal getTotalSalesByStoreAndDateRange(@Param("storeId") UUID storeId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.store.storeId = :storeId " +
            "AND o.status = 'COMPLETED' AND o.completedAt BETWEEN :start AND :end")
    Integer getOrderCountByStoreAndDateRange(@Param("storeId") UUID storeId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);
}
