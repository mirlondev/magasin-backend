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

/**
 * Stratégie pour la génération de documents de remboursement.
 * Applicable aux retours et remboursements.
 */
@Slf4j
@Component("refundDocumentStrategy")
@RequiredArgsConstructor
public class RefundDocumentStrategy implements DocumentStrategy {

    private final DocumentNumberService documentNumberService;

    @Override
    public boolean canGenerate(Order order) {
        // Remboursements pour retours et échanges
        return order.getOrderType() == OrderType.RETURN;
    }

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.REFUND;
    }

    @Override
    public ValidationResult validateForGeneration(Order order) {
        List<String> errors = new ArrayList<>();

        if (order.getItems() == null || order.getItems().isEmpty()) {
            errors.add("Impossible de générer un remboursement sans articles");
        }

        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Le montant du remboursement doit être supérieur à 0");
        }

        // Vérifier qu'il y a une commande originale

//        if (order.getOriginalOrderId() == null) {
//            errors.add("Un remboursement doit être lié à une commande originale");
//        }
        if (order.getOrderId() == null) {
            errors.add("Un remboursement doit être lié à une commande originale");
        }


        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    @Override
    public void prepareDocumentData(Order order) {
        log.debug("Préparation données remboursement pour commande {}", order.getOrderNumber());
        // Calculer les frais de restockage si applicable
        // Déterminer le mode de remboursement
    }

    @Override
    public String generateDocumentNumber() {
        return documentNumberService.generateRefundNumber();
    }

    @Override
    public boolean allowsReprint() {
        return true;
    }

    @Override
    public boolean allowsVoid() {
        return false; // Un remboursement ne peut pas être annulé (créer un nouveau si erreur)
    }
}
