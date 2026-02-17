package org.odema.posnew.config;


import lombok.RequiredArgsConstructor;
import org.odema.posnew.design.strategy.DocumentStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class DocumentPatternsConfiguration {

    /**
     * Map des stratégies de documents pour auto-injection
     */
    @Bean
    public Map<String, DocumentStrategy> documentStrategies(
            DocumentStrategy receiptDocumentStrategy,
            DocumentStrategy invoiceDocumentStrategy,
            DocumentStrategy proformaDocumentStrategy
    ) {
        Map<String, DocumentStrategy> strategies = new HashMap<>();
        strategies.put("receiptDocumentStrategy", receiptDocumentStrategy);
        strategies.put("invoiceDocumentStrategy", invoiceDocumentStrategy);
        strategies.put("proformaDocumentStrategy", proformaDocumentStrategy);
        return strategies;
    }
}