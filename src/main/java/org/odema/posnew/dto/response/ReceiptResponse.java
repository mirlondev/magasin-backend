package org.odema.posnew.dto.response;

import lombok.Builder;
import org.odema.posnew.entity.OrderItem;
import org.odema.posnew.entity.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Builder
public record ReceiptResponse(
        UUID orderId,
        String orderNumber,
        String customerName,
        String cashierName,
        String storeName,
        String storeAddress,
        String storePhone,
        List<OrderItem> items,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal totalPaid,
        BigDecimal remainingAmount,
        BigDecimal changeAmount,
        Map<PaymentMethod, BigDecimal> paymentsByMethod,
        BigDecimal creditAmount,
        String notes,
        LocalDateTime createdAt
) {
}
