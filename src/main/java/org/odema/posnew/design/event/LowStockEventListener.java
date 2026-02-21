package org.odema.posnew.design.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import org.odema.posnew.application.service.NotificationService; // Supposé existant
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class LowStockEventListener {

    // private final NotificationService notificationService; // Décommentez si vous avez ce service

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLowStock(LowStockEvent event) {
        log.warn("ALERTE STOCK BAS : Produit '{}' dans le store '{}'. Quantité: {}/{}",
                event.getProductName(), event.getStoreName(),
                event.getCurrentQuantity(), event.getReorderPoint());

        // Exemple d'action : Envoyer un email au manager du store
        // notificationService.sendLowStockAlert(
        //     event.getStoreId(), event.getProductId(), event.getCurrentQuantity()
        // );
    }
}