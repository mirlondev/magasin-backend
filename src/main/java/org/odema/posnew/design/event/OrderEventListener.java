package org.odema.posnew.design.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.facade.DocumentGenerationFacade;
import org.odema.posnew.design.factory.SaleStrategyFactory;
import org.odema.posnew.design.strategy.SaleStrategy;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.enums.DocumentType;
import org.odema.posnew.entity.enums.OrderType;
import org.odema.posnew.entity.enums.ReceiptType;
import org.odema.posnew.service.InvoiceService;
import org.odema.posnew.service.ReceiptService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener qui réagit aux événements commande
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final DocumentGenerationFacade documentFacade;
    private final SaleStrategyFactory strategyFactory;
    private  final InvoiceService invoiceService;
    private    final ReceiptService receiptService;

    /**
     * Génère automatiquement le document approprié lors de la création
     */
    @Async
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        Order order = event.getOrder();
        log.info("Événement: Commande créée - {}", order.getOrderNumber());

        try {
            SaleStrategy strategy = strategyFactory.getStrategy(
                    order.getOrderType() != null ? order.getOrderType() : OrderType.POS_SALE
            );
            DocumentType docType = strategy.getDocumentType();

            switch (docType) {
                case INVOICE, PROFORMA -> invoiceService.generateInvoice(order.getOrderId());
                case TICKET, RECEIPT -> receiptService.generateReceipt(order.getOrderId(), ReceiptType.SALE);
            }

            log.info("Document {} généré pour commande {}", docType, order.getOrderNumber());
        } catch (Exception e) {
            log.error("Erreur génération document automatique", e);
        }
    }
    /**
     * Actions lors de la complétion d'une commande
     */
    @EventListener
    public void handleOrderCompleted(OrderCompletedEvent event) {
        Order order = event.getOrder();
        log.info("Événement: Commande complétée - {}", order.getOrderNumber());

        // TODO: Envoyer notification email client
        // TODO: Mettre à jour analytics
        // TODO: Déclencher programme fidélité
    }

    /**
     * Actions lors de l'annulation
     */
    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        Order order = event.getOrder();
        log.info("Événement: Commande annulée - {}", order.getOrderNumber());

        // TODO: Notifier équipe
        // TODO: Analyser raisons annulation
    }

    /**
     * Actions lors de réception paiement
     */
    @EventListener
    public void handlePaymentReceived(PaymentReceivedEvent event) {
        Order order = event.getOrder();
        log.info("Événement: Paiement reçu pour commande {}", order.getOrderNumber());

        // TODO: Envoyer reçu par email
        // TODO: Mettre à jour tableau de bord
    }
}
