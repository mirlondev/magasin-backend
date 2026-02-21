package org.odema.posnew.application.dto.response;

import org.odema.posnew.domain.model.enums.OrderStatus;
import org.odema.posnew.domain.model.enums.OrderType;
import org.odema.posnew.domain.model.enums.PaymentMethod;
import org.odema.posnew.domain.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        String orderNumber,

        UUID customerId,
        String customerName,
        String customerEmail,
        String customerPhone,

        UUID cashierId,
        String cashierName,

        UUID storeId,
        String storeName,

        List<OrderItemResponse> items,

        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal globalDiscountAmount,
        BigDecimal totalAmount,

        BigDecimal totalPaid,
        BigDecimal totalCredit,
        BigDecimal remainingAmount,
        BigDecimal changeAmount,
        List<PaymentResponse> payments,

        OrderStatus status,
        @Deprecated PaymentMethod primaryPaymentMethod,
        PaymentStatus paymentStatus,

        Boolean isTaxable,
        BigDecimal taxRate,
        OrderType orderType,
        String notes,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,

        Integer itemCount,
        Boolean canBeRefunded
) {
}
