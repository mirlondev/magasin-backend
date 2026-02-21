package org.odema.posnew.design.strategy.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.strategy.DocumentStrategy;
import org.odema.posnew.design.strategy.ValidationResult;

import org.odema.posnew.domain.model.Invoice;
import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.enums.DocumentType;
import org.odema.posnew.domain.model.enums.OrderType;
import org.odema.posnew.domain.service.DocumentNumberService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Stratégie pour la génération de proformas (devis).
 * Applicable aux devis et propositions commerciales.
 */
@Slf4j
@Component("proformaDocumentStrategy")
@RequiredArgsConstructor
public class ProformaDocumentStrategy implements DocumentStrategy {

    private final DocumentNumberService documentNumberService;

    @Override
    public boolean canGenerate(Order order) {
        // Proformas pour devis et propositions
        return order.getOrderType() == OrderType.PROFORMA;
    }

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.PROFORMA;
    }

    @Override
    public ValidationResult validateForGeneration(Order order) {
        List<String> errors = new ArrayList<>();

        if (order.getItems() == null || order.getItems().isEmpty()) {
            errors.add("Impossible de générer un proforma pour une commande vide");
        }

        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Le montant total doit être supérieur à 0");
        }

        // Un proforma peut être généré sans client (prospect)

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    @Override
    public void prepareDocumentData(Order order) {
        log.debug("Préparation données proforma pour commande {}", order.getOrderNumber());
        // Ajouter la date de validité (30 jours par défaut)
        // Ajouter les conditions commerciales
    }

    @Override
    public String generateDocumentNumber() {
        Invoice invoice  =new  Invoice();
        return documentNumberService.generateProformaNumber(invoice.getInvoiceType());
    }

    @Override
    public boolean allowsReprint() {
        return true;
    }

    @Override
    public boolean allowsVoid() {
        return true; // Un proforma peut être annulé/remplacé
    }
}
