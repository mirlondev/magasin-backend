package org.odema.posnew.design.strategy.impl;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.design.strategy.SaleStrategy;
import org.odema.posnew.design.strategy.ValidationResult;
import org.odema.posnew.dto.request.OrderRequest;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.OrderType;
import org.odema.posnew.entity.enums.PaymentStatus;
import org.springframework.stereotype.Component;
 import  org.odema.posnew.entity.enums.DocumentType;
import java.util.ArrayList;
import java.util.List;









@Slf4j
@Component("posSaleStrategy")
public class PosSaleStrategy implements SaleStrategy {

    @Override
    public ValidationResult validate(OrderRequest request, Order order) {
        List<String> errors = new ArrayList<>();

        if (request.items() == null || request.items().isEmpty()) {
            errors.add("Le panier ne peut pas être vide pour une vente caisse");
        }

        if (order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Le montant total doit être supérieur à 0");
        }

        // Pour vente caisse, on peut avoir ou non un client
        // Pas d'obligation

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    @Override
    public void prepareOrder(Order order, OrderRequest request) {
        log.info("Préparation vente caisse pour commande {}", order.getOrderNumber());

        // Statut initial PENDING
        order.setStatus(OrderStatus.PENDING);

        // Payment status sera UNPAID jusqu'à réception paiement
        order.setPaymentStatus(PaymentStatus.UNPAID);

        // Notes spécifiques vente caisse
        String notes = order.getNotes() != null ? order.getNotes() : "";
        notes = "Vente caisse (POS)\n" + notes;
        order.setNotes(notes.trim());
    }

    @Override
    public void finalizeOrder(Order order) {
        log.info("Finalisation vente caisse {}", order.getOrderNumber());

        // Si totalement payé, marquer comme complété
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(java.time.LocalDateTime.now());
        }
    }

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.TICKET;
    }

    @Override
    public OrderType getOrderType() {
        return OrderType.POS_SALE;
    }

    @Override
    public boolean allowsPartialPayment() {
        return true; // Vente caisse peut avoir paiements multiples
    }
}



