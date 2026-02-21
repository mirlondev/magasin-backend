package org.odema.posnew.application.dto.request;

import org.odema.posnew.domain.enums_old.RefundMethod;
import org.odema.posnew.domain.enums_old.RefundType;

import java.util.List;
import java.util.UUID;

/**
 * DTO pour la création d'un remboursement.
 */
public record RefundRequest(
        UUID orderId,
        RefundType refundType,
        RefundMethod refundMethod,
        String reason,
        String notes,
        List<RefundItemRequest> items
) {
    /**
     * Constructeur compact avec validation
     */
    public RefundRequest {
        if (orderId == null) {
            throw new IllegalArgumentException("L'ID de commande est obligatoire");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("La raison du remboursement est obligatoire");
        }
        if (refundType == null) {
            refundType = RefundType.FULL;
        }
    }
}
