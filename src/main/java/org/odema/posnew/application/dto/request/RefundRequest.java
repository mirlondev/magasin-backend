package org.odema.posnew.application.dto.request;

import org.odema.posnew.domain.model.enums.RefundMethod;
import org.odema.posnew.domain.model.enums.RefundType;

import java.util.List;
import java.util.UUID;

public record RefundRequest(
        UUID orderId,
        RefundType refundType,
        RefundMethod refundMethod,
        String reason,
        String notes,
        List<RefundItemRequest> items
) {
}
