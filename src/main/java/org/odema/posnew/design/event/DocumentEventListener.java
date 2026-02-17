package org.odema.posnew.design.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listener pour tous les événements liés aux documents
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentEventListener {

    /**
     * Réagit à la génération d'un ticket
     */
    @Async
    @EventListener
    public void handleReceiptGenerated(ReceiptGeneratedEvent event) {
        log.info("Événement: Ticket généré - {} pour commande {}",
                event.getReceipt().getReceiptNumber(),
                event.getOrder().getOrderNumber());

        // TODO: Actions asynchrones
        // - Envoyer notification email si demandé
        // - Mettre à jour analytics
        // - Envoyer à imprimante réseau si configuré
    }

    /**
     * Réagit à la génération d'une facture
     */
    @Async
    @EventListener
    public void handleInvoiceGenerated(InvoiceGeneratedEvent event) {
        log.info("Événement: Facture générée - {} pour commande {}",
                event.getInvoice().getInvoiceNumber(),
                event.getOrder().getOrderNumber());

        // TODO: Actions asynchrones
        // - Envoyer facture par email au client
        // - Mettre à jour tableau de bord
        // - Notifier équipe comptabilité
        // - Programmer relance si crédit
    }

    /**
     * Réagit aux réimpressions
     */
    @EventListener
    public void handleDocumentReprinted(DocumentReprintedEvent event) {
        log.info("Événement: Document réimprimé - {} {} (impression #{})",
                event.getDocumentType(),
                event.getDocumentNumber(),
                event.getPrintCount());

        // TODO: Tracer les réimpressions
        // - Audit trail
        // - Statistiques
    }
}
