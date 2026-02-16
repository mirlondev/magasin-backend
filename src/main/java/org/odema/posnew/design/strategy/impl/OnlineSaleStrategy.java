package org.odema.posnew.design.strategy.impl;


import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.strategy.SaleStrategy;
import org.odema.posnew.design.strategy.ValidationResult;
import org.odema.posnew.dto.request.OrderRequest;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.enums.DocumentType;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.OrderType;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("onlineSaleStrategy")
public class OnlineSaleStrategy implements SaleStrategy {

    @Override
    public ValidationResult validate(OrderRequest request, Order order) {
        List<String> errors = new ArrayList<>();

        if (request.items() == null || request.items().isEmpty()) {
            errors.add("Le panier ne peut pas être vide");
        }

        if (order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Le montant total doit être supérieur à 0");
        }

        // Client optionnel pour vente en ligne (peut être guest)

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    @Override
    public void prepareOrder(Order order, OrderRequest request) {
        log.info("Préparation vente en ligne pour commande {}", order.getOrderNumber());

        order.setStatus(OrderStatus.PROCESSING);
        order.setPaymentStatus(PaymentStatus.UNPAID);

        String notes = order.getNotes() != null ? order.getNotes() : "";
        notes = "Vente en ligne\n" + notes;
        order.setNotes(notes.trim());
    }

    @Override
    public void finalizeOrder(Order order) {
        log.info("Finalisation vente en ligne {}", order.getOrderNumber());

        // Logique spécifique vente en ligne
        // TODO: Notification email, préparation livraison, etc.
    }

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.INVOICE;
    }

    @Override
    public OrderType getOrderType() {
        return OrderType.ONLINE;
    }

    @Override
    public boolean allowsPartialPayment() {
        return false; // Vente en ligne: paiement complet requis
    }
}