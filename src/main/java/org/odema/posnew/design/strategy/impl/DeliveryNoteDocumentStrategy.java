package org.odema.posnew.design.strategy.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.strategy.DocumentStrategy;
import org.odema.posnew.design.strategy.ValidationResult;
import org.odema.posnew.entity.Invoice;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.enums.DocumentType;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.OrderType;
import org.odema.posnew.service.DocumentNumberService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Stratégie pour la génération de bons de livraison.
 * Applicable aux livraisons de marchandises.
 */
@Slf4j
@Component("deliveryNoteDocumentStrategy")
@RequiredArgsConstructor
public class DeliveryNoteDocumentStrategy implements DocumentStrategy {

    private final DocumentNumberService documentNumberService;

    @Override
    public boolean canGenerate(Order order) {
        // Bons de livraison pour ventes payées ou expéditions
        return (order.getOrderType() == OrderType.POS_SALE ||
                order.getOrderType() == OrderType.ONLINE ||
                order.getOrderType() == OrderType.CREDIT_SALE) &&
                order.getStatus() != OrderStatus.CANCELLED;
    }

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.DELIVERY_NOTE;
    }

    @Override
    public ValidationResult validateForGeneration(Order order) {
        List<String> errors = new ArrayList<>();

        if (order.getItems() == null || order.getItems().isEmpty()) {
            errors.add("Impossible de générer un bon de livraison sans articles");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            errors.add("Impossible de générer un BL pour une commande annulée");
        }

        // Vérifier l'adresse de livraison
        if (order.getCustomer().getAddress() == null) {
            errors.add("Une adresse de livraison est requise");
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    @Override
    public void prepareDocumentData(Order order) {
        log.debug("Préparation données bon de livraison pour commande {}", order.getOrderNumber());
        // Vérifier le stock disponible
        // Calculer le poids estimé
        // Déterminer le transporteur
    }

    @Override
    public String generateDocumentNumber() {
        Invoice invoice  =new  Invoice();
        return documentNumberService.generateDeliveryNoteNumber(invoice.getInvoiceType());
    }

    @Override
    public boolean allowsReprint() {
        return true;
    }

    @Override
    public boolean allowsVoid() {
        return true; // Un BL peut être annulé si erreur
    }
}
