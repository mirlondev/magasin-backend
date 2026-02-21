package org.odema.posnew.design.factory;


import lombok.RequiredArgsConstructor;
import org.odema.posnew.design.strategy.DocumentStrategy;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.enums.OrderType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentStrategyFactory {

    private final Map<String, DocumentStrategy> documentStrategies;

    /**
     * Récupère la stratégie de document appropriée pour une commande
     */
    public DocumentStrategy getStrategyForOrder(Order order) {
        return documentStrategies.values().stream()
                .filter(strategy -> strategy.canGenerate(order))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Aucune stratégie de document trouvée pour le type: " +
                                order.getOrderType()
                ));
    }

    /**
     * Récupère une stratégie par nom
     */
    public DocumentStrategy getStrategy(String strategyName) {
        DocumentStrategy strategy = documentStrategies.get(strategyName);

        if (strategy == null) {
            throw new BadRequestException(
                    "Stratégie de document non trouvée: " + strategyName
            );
        }

        return strategy;
    }

    /**
     * Récupère la stratégie selon le type de commande
     */
    public DocumentStrategy getStrategyByOrderType(OrderType orderType) {
        String strategyName = switch (orderType) {
            case POS_SALE, ONLINE -> "receiptDocumentStrategy";
            case CREDIT_SALE      -> "invoiceDocumentStrategy";
            case PROFORMA         -> "proformaDocumentStrategy";
            // ✅ CORRIGÉ — ces cas ne plantent plus
            case RETURN           -> "receiptDocumentStrategy";  // ticket de remboursement
            case EXCHANGE         -> "invoiceDocumentStrategy";  // note de crédit
        };
        return getStrategy(strategyName);
    }


}