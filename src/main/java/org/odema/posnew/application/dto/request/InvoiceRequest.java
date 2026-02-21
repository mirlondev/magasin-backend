package org.odema.posnew.application.dto.request;

import org.odema.posnew.domain.model.enums.InvoiceType;

import java.time.LocalDate;
import java.util.UUID;

public record InvoiceRequest(
        UUID orderId,
        InvoiceType invoiceType,
        LocalDate paymentDueDate,
        Integer validityDays,
        String notes
) {
}
