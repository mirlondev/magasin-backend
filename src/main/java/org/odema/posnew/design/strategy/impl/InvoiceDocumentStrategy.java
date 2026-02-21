package org.odema.posnew.design.strategy.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.strategy.DocumentStrategy;
import org.odema.posnew.design.strategy.ValidationResult;

import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.enums.DocumentType;
import org.odema.posnew.domain.model.enums.OrderType;
import org.odema.posnew.domain.service.DocumentNumberService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("invoiceDocumentStrategy")
@RequiredArgsConstructor
public class InvoiceDocumentStrategy implements DocumentStrategy {

    private final DocumentNumberService documentNumberService;

    @Override
    public boolean canGenerate(Order order) {
        // Factures pour ventes à crédit et proforma
        return order.getOrderType() == OrderType.CREDIT_SALE ||
                order.getOrderType() == OrderType.PROFORMA;
    }

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.INVOICE;
    }

    @Override
    public ValidationResult validateForGeneration(Order order) {
        List<String> errors = new ArrayList<>();

        if (order.getCustomer() == null) {
            errors.add("Un client est OBLIGATOIRE pour générer une facture");
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            errors.add("Impossible de générer une facture pour une commande vide");
        }

        if (order.getTotalAmount() == null ||
                order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Le montant total doit être supérieur à 0");
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    @Override
    public void prepareDocumentData(Order order) {
        log.debug("Préparation données facture pour commande {}", order.getOrderNumber());

        // S'assurer que les informations client sont complètes
        if (order.getCustomer() != null) {
            // Validation ou enrichissement des données client
        }
    }

    @Override
    public String generateDocumentNumber() {
        return null; // Sera appelé avec type dans le service
    }

    @Override
    public boolean allowsReprint() {
        return true; // Les factures peuvent être réimprimées
    }

    @Override
    public boolean allowsVoid() {
        return false; // Les factures ne peuvent pas être annulées (crédit note à la place)
    }
}
