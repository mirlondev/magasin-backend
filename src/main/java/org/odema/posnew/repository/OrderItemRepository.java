package org.odema.posnew.repository;

import org.odema.posnew.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrder_OrderId(UUID orderId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.productId = :productId")
    List<OrderItem> findByProductId(@Param("productId") UUID productId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.store.storeId = :storeId")
    List<OrderItem> findByStore(@Param("storeId") String storeId);

    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.product.productId = :productId")
    Integer getTotalQuantitySold(@Param("productId") UUID productId);

    @Query("SELECT SUM(oi.finalPrice) FROM OrderItem oi WHERE oi.order.store.storeId = :storeId AND oi.order.status = 'COMPLETED'")
    Double getTotalRevenueByStore(@Param("storeId") String storeId);
}
