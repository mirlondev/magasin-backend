package org.odema.posnew.application.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.odema.posnew.domain.enums_old.ReceiptStatus;
import org.odema.posnew.domain.enums_old.ReceiptType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReceiptResponse(
        String receiptId,
        String receiptNumber,
        ReceiptType receiptType,
        ReceiptStatus status,
        String orderId,
        String orderNumber,
        String shiftReportId,
        String cashierId,
        String cashierName,
        String storeId,
        String storeName,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime receiptDate,

        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal changeAmount,
        String paymentMethod,
        String pdfUrl,
        Integer printCount,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime lastPrintedAt,

        String notes,
        Boolean isActive,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {}
