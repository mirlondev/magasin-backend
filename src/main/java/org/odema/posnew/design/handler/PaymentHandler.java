package org.odema.posnew.design.handler;
import org.odema.posnew.application.dto.request.PaymentRequest;
import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.Payment;

public interface PaymentHandler {
    /**
     * Définir le prochain handler dans la chaîne
     */
    PaymentHandler setNext(PaymentHandler handler);

    /**
     * Traiter le paiement
     */
    Payment handle(PaymentRequest request, Order order);
}


