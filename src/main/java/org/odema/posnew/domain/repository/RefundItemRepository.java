package org.odema.posnew.application.repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RefundItemRepository extends JpaRepository<RefundItem, UUID> {

    List<RefundItem> findByRefund_RefundId(UUID refundId);

    List<RefundItem> findByOriginalOrderItem_OrderItemId(UUID orderItemId);

    @Query("SELECT COALESCE(SUM(ri.quantity), 0) FROM RefundItem ri " +
            "WHERE ri.product.productId = :productId AND ri.refund.status = 'COMPLETED'")
    Integer sumQuantityByProductAndCompleted(@Param("productId") UUID productId);
}
