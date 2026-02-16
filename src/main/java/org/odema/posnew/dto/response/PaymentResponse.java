package org.odema.posnew.dto.response;

import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID orderId,
        String orderNumber,
        PaymentMethod method,
        BigDecimal amount,
        PaymentStatus status,
        UUID cashierId,
        String cashierName,
        UUID shiftReportId,
        String notes,
        LocalDateTime createdAt
) {}