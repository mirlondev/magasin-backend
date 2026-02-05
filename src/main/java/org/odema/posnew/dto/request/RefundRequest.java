package org.odema.posnew.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.odema.posnew.entity.enums.RefundType;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequest(
        @NotNull(message = "La commande est obligatoire")
        UUID orderId,

        @NotNull(message = "Le montant du remboursement est obligatoire")
        BigDecimal refundAmount,

        RefundType refundType,

        @NotBlank(message = "La raison du remboursement est obligatoire")
        String reason,

        String notes
) {
}
