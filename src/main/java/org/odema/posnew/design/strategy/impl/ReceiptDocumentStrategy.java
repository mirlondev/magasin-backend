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
@Component("receiptDocumentStrategy")
@RequiredArgsConstructor
public class ReceiptDocumentStrategy implements DocumentStrategy {

    private final DocumentNumberService documentNumberService;

    @Override
    public boolean canGenerate(Order order) {
        // Tickets pour ventes POS et ONLINE
        return order.getOrderType() == OrderType.POS_SALE ||
                order.getOrderType() == OrderType.ONLINE;
    }

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.TICKET;
    }

    @Override
    public ValidationResult validateForGeneration(Order order) {
        List<String> errors = new ArrayList<>();

        if (order.getItems() == null || order.getItems().isEmpty()) {
            errors.add("Impossible de générer un ticket pour une commande vide");
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
        log.debug("Préparation données ticket pour commande {}", order.getOrderNumber());
        // Ajout de données spécifiques si nécessaire
    }

    @Override
    public String generateDocumentNumber() {
        // Délégué au service de génération
        return null; // Sera appelé avec storeId dans le service
    }

    @Override
    public boolean allowsReprint() {
        return true; // Les tickets peuvent être réimprimés
    }

    @Override
    public boolean allowsVoid() {
        return true; // Les tickets peuvent être annulés
    }
}
