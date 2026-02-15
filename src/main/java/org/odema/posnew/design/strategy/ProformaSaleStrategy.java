package org.odema.posnew.design.strategy;

import org.odema.posnew.dto.request.OrderRequest;
import org.odema.posnew.entity.Order;

import java.util.List;

@Slf4j
@Component("proformaSaleStrategy")
public class ProformaSaleStrategy implements SaleStrategy {

    @Override
    public ValidationResult validate(OrderRequest request, Order order) {
        List<String> errors = new ArrayList<>();

        if (request.items() == null || request.items().isEmpty()) {
            errors.add("Le panier ne peut pas être vide pour un proforma");
        }

        if (order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            errors.add("Le montant total doit être supérieur à 0");
        }

        // Client optionnel pour proforma

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    @Override
    public void prepareOrder(Order order, OrderRequest request) {
        log.info("Préparation proforma pour commande {}", order.getOrderNumber());

        // Proforma reste en PENDING (c'est un devis)
        order.setStatus(OrderStatus.PENDING);

        // Pas de paiement pour proforma (c'est juste une offre)
        order.setPaymentStatus(PaymentStatus.UNPAID);

        // Notes spécifiques proforma
        String notes = order.getNotes() != null ? order.getNotes() : "";
        if (!notes.contains("PROFORMA") && !notes.contains("Devis")) {
            notes = "PROFORMA / DEVIS\n" + notes;
        }
        order.setNotes(notes.trim());
    }

    @Override
    public void finalizeOrder(Order order) {
        log.info("Finalisation proforma {}", order.getOrderNumber());

        // Proforma reste en PENDING
        // Il devra être converti en vente réelle plus tard
    }

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.PROFORMA;
    }

    @Override
    public OrderType getOrderType() {
        return OrderType.PROFORMA;
    }

    @Override
    public boolean allowsPartialPayment() {
        return false; // Proforma n'a pas de paiement
    }
}
