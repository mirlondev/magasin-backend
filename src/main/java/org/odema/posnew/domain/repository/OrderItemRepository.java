package org.odema.posnew.application.repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    List<OrderItem> findByOrder_OrderId(UUID orderId);

    List<OrderItem> findByProduct_ProductId(UUID productId);
}
