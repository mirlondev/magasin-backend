package org.odema.posnew.config;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.design.handler.PaymentHandler;
import org.odema.posnew.repository.PaymentRepository;
import org.odema.posnew.repository.ShiftReportRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.Bean;
import org.odema.posnew.design.strategy.SaleStrategy;
import org.odema.posnew.design.handler.impl.CashPaymentHandler;
import org.odema.posnew.design.handler.impl.CreditCardPaymentHandler;
import org.odema.posnew.design.handler.impl.MobileMoneyPaymentHandler;
import org.odema.posnew.design.handler.impl.CreditPaymentHandler;
import org.odema.posnew.design.builder.impl.InvoiceDocumentBuilder;
import org.odema.posnew.design.builder.impl.ReceiptDocumentBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration centrale pour tous les design patterns
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class PatternsConfiguration {

    private final PaymentRepository paymentRepository;
    private final ShiftReportRepository shiftReportRepository;

    /**
     * FACTORY PATTERN - Map des stratégies pour auto-injection
     */
    @Bean
    public Map<String, SaleStrategy> saleStrategies(
            SaleStrategy posSaleStrategy,
            SaleStrategy creditSaleStrategy,
             SaleStrategy proformaSaleStrategy,
            SaleStrategy onlineSaleStrategy
    ) {
        Map<String, SaleStrategy> strategies = new HashMap<>();
        strategies.put("posSaleStrategy", posSaleStrategy);
        strategies.put("creditSaleStrategy", creditSaleStrategy);
        strategies.put("proformaSaleStrategy", proformaSaleStrategy);
        strategies.put("onlineSaleStrategy", onlineSaleStrategy);
        return strategies;
    }

    /**
     * CHAIN OF RESPONSIBILITY - Chaîne de handlers de paiement
     */
    @Bean
    public PaymentHandler paymentHandlerChain() {
        // Créer les handlers
        CashPaymentHandler cashHandler = new CashPaymentHandler(
                paymentRepository,
                shiftReportRepository
        );

        CreditCardPaymentHandler cardHandler = new CreditCardPaymentHandler(
                paymentRepository,
                shiftReportRepository
        );

        MobileMoneyPaymentHandler mobileHandler = new MobileMoneyPaymentHandler(
                paymentRepository,
                shiftReportRepository
        );

        CreditPaymentHandler creditHandler = new CreditPaymentHandler(
                paymentRepository
        );

        // Construire la chaîne
        cashHandler.setNext(cardHandler);
        cardHandler.setNext(mobileHandler);
        mobileHandler.setNext(creditHandler);

        return cashHandler; // Retourner le premier maillon
    }

    /**
     * BUILDER PATTERN - Builders de documents (prototype scope)
     */
    @Bean
    public InvoiceDocumentBuilder invoiceDocumentBuilder() {
        return new InvoiceDocumentBuilder();
    }

    @Bean
    public ReceiptDocumentBuilder receiptDocumentBuilder() {
        return new ReceiptDocumentBuilder();
    }
}
