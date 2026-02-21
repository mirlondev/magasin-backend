package org.odema.posnew.application.dto;

import org.odema.posnew.domain.model.enums.InvoiceStatus;
import org.odema.posnew.domain.model.enums.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceResponse(
        String invoiceId,
        String invoiceNumber,
        InvoiceType invoiceType,
        InvoiceStatus status,

        String orderId,
        String orderNumber,

        String customerId,
        String customerName,

        String storeId,
        String storeName,

        LocalDate invoiceDate,
        LocalDate paymentDueDate,
        Integer validityDays,

        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal amountDue,

        String paymentMethod,
        String pdfUrl,

        Integer printCount,
        LocalDateTime lastPrintedAt,
        Boolean convertedToSale,
        LocalDateTime convertedAt,
        String convertedOrderId,

        String notes,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
