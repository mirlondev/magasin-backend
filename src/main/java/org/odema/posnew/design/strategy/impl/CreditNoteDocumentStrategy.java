package org.odema.posnew.design.strategy.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.strategy.DocumentStrategy;
import org.odema.posnew.design.strategy.ValidationResult;
import org.odema.posnew.entity.Invoice;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.enums.DocumentType;
import org.odema.posnew.entity.enums.OrderType;
import org.odema.posnew.service.DocumentNumberService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Stratégie pour la génération de notes de crédit (avoirs).
 * Applicable aux avoirs et corrections de factures.
 */
@Slf4j
@Component("creditNoteDocumentStrategy")
@RequiredArgsConstructor
public class CreditNoteDocumentStrategy implements DocumentStrategy {

    private final DocumentNumberService documentNumberService;

    @Override
    public boolean canGenerate(Order order) {
        // Notes de crédit pour échanges et corrections
        return order.getOrderType() == OrderType.EXCHANGE ||
                order.getOrderType() == OrderType.RETURN;
    }

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.CREDIT_NOTE;
    }

    @Override
    public ValidationResult validateForGeneration(Order order) {
        List<String> errors = new ArrayList<>();

        if (order.getItems() == null || order.getItems().isEmpty()) {
            errors.add("Impossible de générer une note de crédit sans articles");
        }

        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Le montant de l'avoir doit être supérieur à 0");
        }

        // Vérifier qu'il y a une facture originale
        if (order.getOriginalOrderId() == null) {
            errors.add("Une note de crédit doit être liée à une facture originale");
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    @Override
    public void prepareDocumentData(Order order) {
        log.debug("Préparation données note de crédit pour commande {}", order.getOrderNumber());
        // Récupérer la facture originale
        // Calculer les ajustements TVA
        // Déterminer si l'avoir est utilisable ou remboursable
    }

    @Override
    public String generateDocumentNumber() {
        Invoice invoice  =new  Invoice();
        return documentNumberService.generateCreditNoteNumber(invoice.getInvoiceType());
    }

    @Override
    public boolean allowsReprint() {
        return true;
    }

    @Override
    public boolean allowsVoid() {
        return false; // Une note de crédit ne peut pas être annulée (créer une facture de rectification)
    }
}
