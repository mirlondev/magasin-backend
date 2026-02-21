package org.odema.posnew.design.handler;

import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.application.dto.request.PaymentRequest;
import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.Payment;
import org.odema.posnew.domain.model.enums.PaymentMethod;

@Slf4j
public abstract class AbstractPaymentHandler implements PaymentHandler {

    protected PaymentHandler nextHandler;

    @Override
    public PaymentHandler setNext(PaymentHandler handler) {
        this.nextHandler = handler;
        return handler;
    }

    @Override
    public Payment handle(PaymentRequest request, Order order) {
        if (canHandle(request.method())) {
            log.info("Handler {} traite paiement {}",
                    getClass().getSimpleName(), request.method());
            return processPayment(request, order);
        }

        if (nextHandler != null) {
            return nextHandler.handle(request, order);
        }

        throw new IllegalStateException(
                "Aucun handler disponible pour: " + request.method()
        );
    }

    /**
     * Vérifie si ce handler peut traiter le paiement
     */
    protected abstract boolean canHandle(PaymentMethod method);

    /**
     * Traitement effectif du paiement
     */
    protected abstract Payment processPayment(PaymentRequest request, Order order);
}
