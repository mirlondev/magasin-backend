package org.odema.posnew.mapper;

import org.odema.posnew.dto.response.PaymentResponse;
import org.odema.posnew.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrder() != null ? payment.getOrder().getOrderId() : null,
                payment.getOrder() != null ? payment.getOrder().getOrderNumber() : null,
                payment.getMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCashier() != null ? payment.getCashier().getUserId() : null,
                payment.getCashier() != null ? payment.getCashier().getUsername() : null,
                payment.getShiftReport() != null ? payment.getShiftReport().getShiftReportId() : null,
                payment.getNotes(),
                payment.getCreatedAt()
        );
    }

    /**
     * toEntity n'est PAS utilisé - Payment doit toujours être créé via PaymentService
     * qui applique les règles métier
     */
    public Payment toEntity() {
        throw new UnsupportedOperationException(
                "Payment doit être créé via PaymentService (règles métier)"
        );
    }
}