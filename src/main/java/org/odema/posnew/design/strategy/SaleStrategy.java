package org.odema.posnew.design.strategy;



import org.odema.posnew.application.dto.request.OrderRequest;
import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.enums.DocumentType;
import org.odema.posnew.domain.model.enums.OrderType;


public interface SaleStrategy {
    /**
     * Valide la commande selon les règles métier du type de vente
     */
    ValidationResult validate(OrderRequest request, Order order);

    /**
     * Prépare la commande selon le type de vente
     */
    void prepareOrder(Order order, OrderRequest request);

    /**
     * Finalise la commande après création
     */
    void finalizeOrder(Order order);

    /**
     * Type de document à générer
     */
    DocumentType getDocumentType();

    /**
     * Type de commande
     */
    OrderType getOrderType();

    /**
     * Autorise les paiements partiels
     */
    boolean allowsPartialPayment();
}

