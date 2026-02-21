package org.odema.posnew.design.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class PaymentReceivedEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentReceived(PaymentReceivedEvent event) {
        log.info("Paiement reçu (post-commit): commande={}, montant={}",
                event.getOrder().getOrderNumber(),
                event.getPayment().getAmount());
    }
}
