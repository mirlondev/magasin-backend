package org.odema.posnew.repository;

import org.odema.posnew.entity.RefundItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour les articles remboursés.
 */
@Repository
public interface RefundItemRepository extends JpaRepository<RefundItem, UUID> {

    List<RefundItem> findByRefund_RefundId(UUID refundId);

    List<RefundItem> findByOriginalOrderItem_OrderItemId(UUID orderItemId);

    List<RefundItem> findByProduct_ProductId(UUID productId);

    @Query("SELECT ri FROM RefundItem ri WHERE ri.refund.order.orderId = :orderId")
    List<RefundItem> findByOrderId(@Param("orderId") UUID orderId);

    @Query("SELECT SUM(ri.quantity) FROM RefundItem ri WHERE ri.product.productId = :productId AND ri.refund.status = 'COMPLETED'")
    Integer sumQuantityByProductAndCompleted(@Param("productId") UUID productId);

    @Query("SELECT ri FROM RefundItem ri WHERE ri.isReturned = false AND ri.refund.status = 'APPROVED'")
    List<RefundItem> findPendingReturns();

    @Query("SELECT COUNT(ri) FROM RefundItem ri WHERE ri.refund.refundId = :refundId")
    Long countByRefundId(@Param("refundId") UUID refundId);
}
