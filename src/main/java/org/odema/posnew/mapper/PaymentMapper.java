package org.odema.posnew.mapper;

import org.odema.posnew.dto.response.PaymentResponse;
import org.odema.posnew.entity.Payment;
import org.springframework.stereotype.Component;

import static org.odema.posnew.mapper.OrderMapper.getPaymentResponse;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return getPaymentResponse(payment);
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