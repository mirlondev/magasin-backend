package org.odema.posnew.design.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class StockAdjustmentEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockAdjusted(StockAdjustmentEvent event) {
        log.info("AJUSTEMENT STOCK : Produit '{}' ({}) - Opération: {} de {} unités. Nouveau total: {}. Raison: {}",
                event.getProductName(), event.getProductId(),
                event.getOperation(), event.getQuantityChanged(),
                event.getNewTotalQuantity(), event.getReason());

        // Ici, vous pourriez persister un historique dans une table 'StockMovementLog'
    }
}