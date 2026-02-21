package org.odema.posnew.application.dto.response;

import org.odema.posnew.domain.enums_old.RefundMethod;
import org.odema.posnew.domain.enums_old.RefundStatus;
import org.odema.posnew.domain.enums_old.RefundType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de réponse pour un remboursement.
 */
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
        Integer totalItems,
        LocalDateTime approvedAt,
        LocalDateTime processedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
    /**
     * Constructeur avec calcul des totaux
     */
    public RefundResponse {
        if (items != null) {
            totalItems = items.stream()
                    .mapToInt(RefundItemResponse::quantity)
                    .sum();
        }
    }
}
