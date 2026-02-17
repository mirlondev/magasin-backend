package org.odema.posnew.design.strategy.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.strategy.DocumentStrategy;
import org.odema.posnew.design.strategy.ValidationResult;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.enums.DocumentType;
import org.odema.posnew.entity.enums.OrderType;
import org.odema.posnew.service.DocumentNumberService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("proformaDocumentStrategy")
@RequiredArgsConstructor
public class ProformaDocumentStrategy implements DocumentStrategy {

    private final DocumentNumberService documentNumberService;

    @Override
    public boolean canGenerate(Order order) {
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
            errors.add("Impossible de générer un proforma vide");
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
        log.debug("Préparation données proforma pour commande {}", order.getOrderNumber());

        // Ajouter validité si pas présente
        String notes = order.getNotes() != null ? order.getNotes() : "";
        if (!notes.contains("Validité:")) {
            int validityDays = 30; // Par défaut 30 jours
            notes += "\nValidité: " + validityDays + " jours";
            order.setNotes(notes.trim());
        }
    }

    @Override
    public String generateDocumentNumber() {
        return null;
    }

    @Override
    public boolean allowsReprint() {
        return true;
    }

    @Override
    public boolean allowsVoid() {
        return true; // Les proforma peuvent être annulés
    }
}
