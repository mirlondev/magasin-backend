package org.odema.posnew.design.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.dto.request.PaymentRequest;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.Payment;
import org.odema.posnew.entity.ShiftReport;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.repository.PaymentRepository;
import org.odema.posnew.repository.ShiftReportRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class CashPaymentHandler extends AbstractPaymentHandler {

    private final PaymentRepository paymentRepository;
    private final ShiftReportRepository shiftReportRepository;

    @Override
    protected boolean canHandle(PaymentMethod method) {
        return method == PaymentMethod.CASH;
    }

    @Override
    protected Payment processPayment(PaymentRequest request, Order order) {
        log.info("Traitement paiement ESPÈCES: {} pour commande {}",
                request.amount(), order.getOrderNumber());

        // Validation montant
        validateAmount(request.amount(), order);

        // Récupérer shift ouvert
        ShiftReport shift = shiftReportRepository
                .findOpenShiftByCashier(order.getCashier().getUserId())
                .orElseThrow(() -> new BadRequestException(
                        "Aucune session de caisse ouverte"
                ));

        // Créer paiement
        Payment payment = Payment.builder()
                .order(order)
                .method(PaymentMethod.CASH)
                .amount(request.amount())
                .cashier(order.getCashier())
                .shiftReport(shift)
                .status(PaymentStatus.PAID)
                .notes(request.notes())
                .isActive(true)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Mettre à jour shift
        shift.addSale(request.amount());
        shiftReportRepository.save(shift);

        log.info("Paiement espèces enregistré: ID {}", savedPayment.getPaymentId());

        return savedPayment;
    }

    private void validateAmount(BigDecimal amount, Order order) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Le montant doit être positif");
        }

        BigDecimal remaining = order.getRemainingAmount();
        if (amount.compareTo(remaining) > 0) {
            log.warn("Paiement supérieur au montant dû (surpaiement): {} > {}",
                    amount, remaining);
            // Autorisé pour espèces (monnaie à rendre)
        }
    }
}
