package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.response.PaymentResponse;
import org.odema.posnew.domain.model.Payment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) return null;

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

    public List<PaymentResponse> toResponseList(List<Payment> payments) {
        if (payments == null) return List.of();
        return payments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}