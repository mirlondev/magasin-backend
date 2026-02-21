package org.odema.posnew.application.dto.request;

import org.odema.posnew.domain.model.enums.ReceiptType;

import java.util.UUID;

public record ReceiptRequest(
        UUID orderId,
        ReceiptType receiptType,
        String notes
) {
}
