package org.odema.posnew.design.factory;


import lombok.RequiredArgsConstructor;
import org.odema.posnew.design.strategy.SaleStrategy;
import org.odema.posnew.entity.enums.OrderType;
import org.odema.posnew.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SaleStrategyFactory {

    private final Map<String, SaleStrategy> strategies;

    /**
     * Récupère la stratégie appropriée selon le type de vente
     */
    public SaleStrategy getStrategy(OrderType orderType) {
        if (orderType == null) {
            // Par défaut, vente caisse
            orderType = OrderType.POS_SALE;
        }

        String strategyName = switch (orderType) {
            case POS_SALE -> "poseSaleStrategy";
            case CREDIT_SALE -> "creditSaleStrategy";
            case PROFORMA -> "proformaSaleStrategy";
            case ONLINE -> "onlineSaleStrategy";
        };

        SaleStrategy strategy = strategies.get(strategyName);

        if (strategy == null) {
            throw new BadRequestException(
                    "Aucune stratégie trouvée pour le type: " + orderType
            );
        }

        return strategy;
    }
}