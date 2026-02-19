package org.odema.posnew.design.builder;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Configuration centralisée pour les builders de documents.
 * Les valeurs sont injectées depuis application.properties/yaml
 */
@Getter
@Setter
@Component
public class DocumentBuilderConfig {

    // Informations société
    @Value("${app.company.name:ODEMA POS}")
    private String companyName;

    @Value("${app.company.address:123 Rue Principale, Pointe-Noire}")
    private String companyAddress;

    @Value("${app.company.phone:+237 6XX XX XX XX}")
    private String companyPhone;

    @Value("${app.company.email:contact@odema.com}")
    private String companyEmail;

    @Value("${app.company.tax-id:TAX-123456789}")
    private String companyTaxId;

    @Value("${app.company.rccm:RCCM-XX-XXX}")
    private String companyRccm;

    @Value("${app.company.bank-name:Banque Atlantique}")
    private String companyBankName;

    @Value("${app.company.bank-account:CM21 1234 5678 9012 3456 7890 123}")
    private String companyBankAccount;

    @Value("${app.company.website:www.odema.com}")
    private String companyWebsite;

    @Value("${app.company.logo-path:static/logo.png}")
    private String companyLogoPath;

    // Messages de pied de page
    @Value("${app.receipt.footer-message:Merci de votre visite !}")
    private String receiptFooterMessage;

    @Value("${app.refund.footer-message:Merci de votre confiance !}")
    private String refundFooterMessage;

    @Value("${app.cancellation.footer-message:Cette commande a été annulée}")
    private String cancellationFooterMessage;

    @Value("${app.invoice.footer-message:Merci pour votre confiance}")
    private String invoiceFooterMessage;

    // Configuration des formats
    @Value("${app.document.thermal-width:80mm}")
    private String thermalWidth;

    @Value("${app.document.a4-margins:12mm}")
    private String a4Margins;

    @Value("${app.document.currency:FCFA}")
    private String currencyCode;

    @Value("${app.document.currency-symbol:F}")
    private String currencySymbol;

    // Configuration TVA
    @Value("${app.tax.default-rate:18.00}")
    private BigDecimal defaultTaxRate;

    @Value("${app.tax.included:true}")
    private boolean taxIncluded;

    // Configuration des numéros de document
    @Value("${app.document.receipt-prefix:RCP}")
    private String receiptPrefix;

    @Value("${app.document.invoice-prefix:INV}")
    private String invoicePrefix;

    @Value("${app.document.proforma-prefix:PRO}")
    private String proformaPrefix;

    @Value("${app.document.credit-note-prefix:AVO}")
    private String creditNotePrefix;

    @Value("${app.document.refund-prefix:RMB}")
    private String refundPrefix;
}
