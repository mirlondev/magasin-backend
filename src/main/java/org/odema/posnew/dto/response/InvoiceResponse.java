
package org.odema.posnew.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.odema.posnew.entity.enums.InvoiceStatus;
import org.odema.posnew.entity.enums.InvoiceType;

import java.math.BigDecimal;
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

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime invoiceDate,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime paymentDueDate,

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

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastPrintedAt,

        Boolean convertedToSale,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime convertedAt,

        String convertedOrderId,
        String notes,
        Boolean isActive,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {}

