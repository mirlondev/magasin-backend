package org.odema.posnew.design.context;

import lombok.Builder;
import lombok.Getter;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.Refund;

@Builder
@Getter
public class DocumentBuildContext {
    private final Order order;
    private final Refund refund;
    private final String cancellationReason;
    private final String cancelledBy;

    // Factory methods pour chaque cas
    public static DocumentBuildContext forOrder(Order order) {
        return DocumentBuildContext.builder().order(order).build();
    }

    public static DocumentBuildContext forCancellation(Order order, String reason, String cancelledBy) {
        return DocumentBuildContext.builder()
                .order(order)
                .cancellationReason(reason)
                .cancelledBy(cancelledBy)
                .build();
    }

    public static DocumentBuildContext forRefund(Order order, Refund refund) {
        return DocumentBuildContext.builder()
                .order(order)
                .refund(refund)
                .build();
    }

    public static DocumentBuildContext forCreditNote(Order order, Refund refund) {
        return DocumentBuildContext.builder()
                .order(order)
                .refund(refund)
                .build();
    }
}