package org.odema.posnew.mapper;

import org.odema.posnew.dto.response.RefundItemResponse;
import org.odema.posnew.dto.response.RefundResponse;
import org.odema.posnew.entity.Refund;
import org.odema.posnew.entity.RefundItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper pour les remboursements.
 */
@Component
public class RefundMapper {

    public RefundResponse toResponse(Refund refund) {
        if (refund == null) return null;

        List<RefundItemResponse> itemResponses = refund.getItems() != null
                ? refund.getItems().stream()
                        .map(this::toItemResponse)
                        .collect(Collectors.toList())
                : List.of();

        return new RefundResponse(
                refund.getRefundId(),
                refund.getRefundNumber(),
                refund.getOrder() != null ? refund.getOrder().getOrderId() : null,
                refund.getOrder() != null ? refund.getOrder().getOrderNumber() : null,
                refund.getRefundType(),
                refund.getStatus(),
                refund.getRefundMethod(),
                refund.getRefundAmount(),
                refund.getTotalRefundAmount(),
                refund.getRestockingFee(),
                refund.getReason(),
                refund.getNotes(),
                refund.getCashier() != null ? refund.getCashier().getUserId() : null,
                refund.getCashier() != null ? refund.getCashier().getUsername() : null,
                refund.getStore() != null ? refund.getStore().getStoreId() : null,
                refund.getStore() != null ? refund.getStore().getName() : null,
                refund.getShiftReport() != null ? refund.getShiftReport().getShiftReportId() : null,
                itemResponses,
                itemResponses.size(),
                refund.getApprovedAt(),
                refund.getProcessedAt(),
                refund.getCompletedAt(),
                refund.getCreatedAt()
        );
    }

    public RefundItemResponse toItemResponse(RefundItem item) {
        if (item == null) return null;

        return new RefundItemResponse(
                item.getRefundItemId(),
                item.getOriginalOrderItem() != null ? item.getOriginalOrderItem().getOrderItemId() : null,
                item.getProduct() != null ? item.getProduct().getProductId() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getProduct() != null ? item.getProduct().getSku() : null,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getRefundAmount(),
                item.getRestockingFee(),
                item.getNetRefundAmount(),
                item.getReason(),
                item.getIsReturned(),
                item.getReturnedAt(),
                item.getIsExchange(),
                item.getExchangeProduct() != null ? item.getExchangeProduct().getProductId() : null,
                item.getExchangeProduct() != null ? item.getExchangeProduct().getName() : null,
                item.getExchangeQuantity()
        );
    }

    public List<RefundResponse> toResponseList(List<Refund> refunds) {
        return refunds.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}
