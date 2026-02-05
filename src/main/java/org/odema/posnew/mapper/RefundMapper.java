package org.odema.posnew.mapper;

import org.odema.posnew.entity.Refund;
import org.odema.posnew.dto.response.RefundResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RefundMapper {

    public RefundResponse toResponse(Refund refund) {
        if (refund == null) return null;

        return new RefundResponse(
                refund.getRefundId(),
                refund.getRefundNumber(),

                refund.getOrder() != null ? refund.getOrder().getOrderId() : null,
                refund.getOrder() != null ? refund.getOrder().getOrderNumber() : null,
                refund.getOrder() != null ? refund.getOrder().getTotalAmount() : null,

                refund.getRefundAmount(),
                refund.getRefundType(),
                refund.getStatus(),
                refund.getReason(),

                refund.getCashier() != null ? refund.getCashier().getUserId() : null,
                refund.getCashier() != null ? refund.getCashier().getUsername() : null,

                refund.getStore() != null ? UUID.fromString(String.valueOf(refund.getStore().getStoreId())) : null,
                refund.getStore() != null ? refund.getStore().getName() : null,

                refund.getShiftReport() != null ? refund.getShiftReport().getShiftReportId() : null,
                refund.getShiftReport() != null ? refund.getShiftReport().getShiftNumber() : null,

                refund.getNotes(),

                refund.getCreatedAt(),
                refund.getProcessedAt(),
                refund.getCompletedAt(),

                refund.getIsActive()
        );
    }
}
