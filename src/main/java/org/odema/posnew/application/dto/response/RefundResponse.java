package org.odema.posnew.application.dto.response;

import org.odema.posnew.domain.model.enums.RefundMethod;
import org.odema.posnew.domain.model.enums.RefundStatus;
import org.odema.posnew.domain.model.enums.RefundType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RefundResponse(
        UUID refundId,
        String refundNumber,

        UUID orderId,
        String orderNumber,

        RefundType refundType,
        RefundStatus status,
        RefundMethod refundMethod,

        BigDecimal refundAmount,
        BigDecimal totalRefundAmount,
        BigDecimal restockingFee,

        String reason,
        String notes,

        UUID cashierId,
        String cashierName,

        UUID storeId,
        String storeName,

        UUID shiftReportId,

        List<RefundItemResponse> items,
        Integer itemCount,

        LocalDateTime approvedAt,
        LocalDateTime processedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
