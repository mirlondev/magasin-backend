package org.odema.posnew.dto.response;

import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;

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
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal changeAmount,

        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,

        Boolean isTaxable,
        BigDecimal taxRate,

        String notes,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,

        Integer itemCount,
        Boolean canBeRefunded
) {
}
