package org.odema.posnew.design.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.domain.service.LoyaltyService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCompletedEventListener {

    private final LoyaltyService loyaltyService;

    // ✅ AFTER_COMMIT = publié seulement si la transaction a réussi
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCompleted(OrderCompletedEvent event) {
        var order = event.getOrder();
        if (order.getCustomer() == null) return;

        try {
            loyaltyService.awardPointsForPurchase(
                    order.getCustomer().getCustomerId(),
                    order.getTotalAmount(),
                    order.getOrderId()
            );
            log.info("Points fidélité attribués pour commande {}: {}",
                    order.getOrderNumber(), order.getTotalAmount());
        } catch (Exception e) {
            // Ne pas faire échouer l'événement — les points peuvent être
            // rattrapés manuellement via adjustPointsManually()
            log.error("Échec attribution points fidélité pour commande {}: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }
}
