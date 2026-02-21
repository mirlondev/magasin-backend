package org.odema.posnew.design.handler.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.application.dto.request.PaymentRequest;
import org.odema.posnew.design.handler.AbstractPaymentHandler;

import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.Payment;
import org.odema.posnew.domain.model.ShiftReport;
import org.odema.posnew.domain.model.enums.PaymentMethod;
import org.odema.posnew.domain.model.enums.PaymentStatus;
import org.odema.posnew.domain.repository.PaymentRepository;
import org.odema.posnew.domain.repository.ShiftReportRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class MobileMoneyPaymentHandler extends AbstractPaymentHandler {

    private final PaymentRepository paymentRepository;
    private final ShiftReportRepository shiftReportRepository;

    @Override
    protected boolean canHandle(PaymentMethod method) {
        return method == PaymentMethod.MOBILE_MONEY;
    }

    @Override
    protected Payment processPayment(PaymentRequest request, Order order) {
        log.info("Traitement paiement MOBILE MONEY: {} pour commande {}",
                request.amount(), order.getOrderNumber());

        validateAmount(request.amount(), order);

        ShiftReport shift = shiftReportRepository
                .findOpenShiftByCashier(order.getCashier().getUserId())
                .orElseThrow(() -> new BadRequestException(
                        "Aucune session de caisse ouverte"
                ));

        // TODO: Intégration API Mobile Money (MTN, Orange, etc.)

        Payment payment = Payment.builder()
                .order(order)
                .method(PaymentMethod.MOBILE_MONEY)
                .amount(request.amount())
                .cashier(order.getCashier())
                .shiftReport(shift)
                .status(PaymentStatus.PAID)
                .notes(request.notes())
                .isActive(true)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        shift.addSale(request.amount(), request.method());
        shiftReportRepository.save(shift);

        log.info("Paiement Mobile Money enregistré: ID {}", savedPayment.getPaymentId());

        return savedPayment;
    }

    private void validateAmount(BigDecimal amount, Order order) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Le montant doit être positif");
        }

        BigDecimal remaining = order.getRemainingAmount();
        if (amount.compareTo(remaining) > 0) {
            throw new BadRequestException(
                    "Le montant ne peut pas dépasser le montant dû"
            );
        }
    }
}