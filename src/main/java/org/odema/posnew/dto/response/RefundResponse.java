package org.odema.posnew.dto.response;

import org.odema.posnew.entity.enums.RefundStatus;
import org.odema.posnew.entity.enums.RefundType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record RefundResponse(
        UUID refundId,
        String refundNumber,

        UUID orderId,
        String orderNumber,
        BigDecimal orderTotal,

        BigDecimal refundAmount,
        RefundType refundType,
        RefundStatus status,
        String reason,

        UUID cashierId,
        String cashierName,

        UUID storeId,
        String storeName,

        UUID shiftReportId,
        String shiftNumber,

        String notes,

        LocalDateTime createdAt,
        LocalDateTime processedAt,
        LocalDateTime completedAt,

        Boolean isActive
) {
}
