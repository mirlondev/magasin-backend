package org.odema.posnew.design.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class StockTransferEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockTransferred(StockTransferEvent event) {
        log.info("TRANSFERT STOCK : {} unités de '{}' déplacées de '{}' vers '{}'",
                event.getQuantity(), event.getProductName(),
                event.getFromStoreName(), event.getToStoreName());

        // Notification au store de destination pour préparer la réception
    }
}