package org.odema.posnew.dto.response;

import org.odema.posnew.entity.enums.InvoiceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceResponse(
        UUID invoiceId,
        String invoiceNumber,

        UUID orderId,
        String orderNumber,

        UUID customerId,
        String customerName,
        String customerEmail,
        String customerPhone,

        UUID storeId,
        String storeName,

        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal amountDue,

        InvoiceStatus status,
        String paymentMethod,

        LocalDateTime invoiceDate,
        LocalDateTime paymentDueDate,

        String pdfFilename,
        String pdfUrl,

        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,

        Boolean isActive,
        Boolean isPaid,
        Boolean isOverdue
) {
}
