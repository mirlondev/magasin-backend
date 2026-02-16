package org.odema.posnew.dto.response;

import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.OrderType;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Order response with detailed payment information
 */
public record OrderResponse(
        // Identifiers
        UUID orderId,
        String orderNumber,

        // Customer
        UUID customerId,
        String customerName,
        String customerEmail,
        String customerPhone,

        // Cashier
        UUID cashierId,
        String cashierName,

        // Store
        UUID storeId,
        String storeName,

        // Items
        List<OrderItemResponse> items,

        // Amounts (calculated from items)
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,

        // Payment info (calculated from payments)
        BigDecimal amountPaid,        // Total paid (deprecated, use totalPaid)
        BigDecimal totalPaid,         // NEW: Sum of non-credit payments
        BigDecimal totalCredit,       // NEW: Sum of credit payments
        BigDecimal remainingAmount,   // NEW: Amount still owed
        BigDecimal changeAmount,
        List<PaymentResponse> payments, // NEW: List of payments

        // Status
        OrderStatus status,
        @Deprecated
        PaymentMethod paymentMethod,  // Deprecated - use payments array
        PaymentStatus paymentStatus,

        // Options
        Boolean isTaxable,
        BigDecimal taxRate,
        OrderType orderType,
        String notes,

        // Dates
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,

        // Counters
        Integer itemCount,
        Boolean canBeRefunded
) {}