package org.odema.posnew.design.strategy;


import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.dto.request.OrderRequest;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.OrderType;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.springframework.stereotype.Component;
import org.w3c.dom.DocumentType;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("creditSaleStrategy")
public class CreditSaleStrategy implements SaleStrategy {

    @Override
    public ValidationResult validate(OrderRequest request, Order order) {
        List<String> errors = new ArrayList<>();

        if (request.items() == null || request.items().isEmpty()) {
            errors.add("Le panier ne peut pas être vide");
        }

        if (order.getCustomer() == null) {
            errors.add("Un client est OBLIGATOIRE pour une vente à crédit");
        }

        if (order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Le montant total doit être supérieur à 0");
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    @Override
    public void prepareOrder(Order order, OrderRequest request) {
        log.info("Préparation vente à crédit pour commande {}", order.getOrderNumber());

        // Statut initial PENDING (en attente de paiement)
        order.setStatus(OrderStatus.PENDING);

        // Payment status UNPAID initialement
        // Sera changé en CREDIT après ajout du paiement crédit
        order.setPaymentStatus(PaymentStatus.UNPAID);

        // Notes spécifiques crédit
        String notes = order.getNotes() != null ? order.getNotes() : "";
        if (!notes.contains("Vente à crédit")) {
            notes = "Vente à crédit\n" + notes;
        }
        order.setNotes(notes.trim());
    }

    @Override
    public void finalizeOrder(Order order) {
        log.info("Finalisation vente à crédit {}", order.getOrderNumber());

        // Pour vente crédit, on garde en PENDING jusqu'à paiement total
        // Le statut sera mis à jour lors des paiements
    }

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.INVOICE; // Facture obligatoire pour crédit
    }

    @Override
    public OrderType getOrderType() {
        return OrderType.CREDIT_SALE;
    }

    @Override
    public boolean allowsPartialPayment() {
        return true; // Crédit permet paiements échelonnés
    }
}

