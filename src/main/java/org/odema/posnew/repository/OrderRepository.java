package org.odema.posnew.repository;

import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByStore_StoreId(UUID storeId, Pageable pageable);
    List<Order> findByStore_StoreId(UUID storeId);

    List<Order> findByCustomer_CustomerId(UUID customerId);

    List<Order> findByCashier_UserId(UUID cashierId);
    Page<Order> findByCashier_UserId(UUID cashierId, Pageable pageable);
    List<Order> findByStatus(OrderStatus status);

    List<Order> findByPaymentStatus(PaymentStatus paymentStatus);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.store.storeId = :storeId AND o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findByStoreAndDateRange(
            @Param("storeId") String storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.store.storeId = :storeId AND o.createdAt BETWEEN :startDate AND :endDate AND o.status = 'COMPLETED'")
    BigDecimal getTotalSalesByStoreAndDateRange(
            @Param("storeId") String storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.store.storeId = :storeId AND o.createdAt BETWEEN :startDate AND :endDate AND o.status = 'COMPLETED'")
    Integer getOrderCountByStoreAndDateRange(
            @Param("storeId") String storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.status = 'COMPLETED' ORDER BY o.createdAt DESC")
    List<Order> findRecentCompletedOrders();

    @Query("SELECT o FROM Order o WHERE o.customer.customerId = :customerId ORDER BY o.createdAt DESC")
    List<Order> findCustomerOrderHistory(@Param("customerId") UUID customerId);

    boolean existsByOrderNumber(String orderNumber);
}
