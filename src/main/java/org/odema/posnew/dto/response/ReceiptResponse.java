package org.odema.posnew.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.odema.posnew.entity.Invoice;
import org.odema.posnew.entity.enums.InvoiceStatus;
import org.odema.posnew.entity.enums.InvoiceType;
import org.odema.posnew.entity.enums.ReceiptStatus;
import org.odema.posnew.entity.enums.ReceiptType;
import org.odema.posnew.service.FileStorageService;
import org.springframework.stereotype.Component;

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
