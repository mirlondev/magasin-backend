package org.odema.posnew.application.dto.response;

import org.odema.posnew.domain.model.enums.PaymentMethod;
import org.odema.posnew.domain.model.enums.ReceiptStatus;
import org.odema.posnew.domain.model.enums.ReceiptType;

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

        LocalDateTime receiptDate,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal changeAmount,
        PaymentMethod paymentMethod,

        String pdfUrl,
        Integer printCount,
        LocalDateTime lastPrintedAt,

        String notes,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
