package org.odema.posnew.design.handler.impl;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.handler.AbstractPaymentHandler;
import org.odema.posnew.dto.request.PaymentRequest;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.Payment;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.repository.PaymentRepository;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditPaymentHandler extends AbstractPaymentHandler {

    private final PaymentRepository paymentRepository;

    @Override
    protected boolean canHandle(PaymentMethod method) {
        return method == PaymentMethod.CREDIT;
    }

    @Override
    protected Payment processPayment(PaymentRequest request, Order order) {
        log.info("Enregistrement CRÉDIT: {} pour commande {}",
                request.amount(), order.getOrderNumber());

        // Vérifier qu'un client est associé
        if (order.getCustomer() == null) {
            throw new BadRequestException(
                    "Un client est requis pour un paiement à crédit"
            );
        }

        // Créer paiement crédit
        Payment payment = Payment.builder()
                .order(order)
                .method(PaymentMethod.CREDIT)
                .amount(request.amount())
                .cashier(order.getCashier())
                .status(PaymentStatus.CREDIT) // Status spécial pour crédit
                .notes(request.notes())
                .isActive(true)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        log.info("Crédit enregistré: ID {} pour client {}",
                savedPayment.getPaymentId(),
                order.getCustomer().getFullName());

        return savedPayment;
    }
}