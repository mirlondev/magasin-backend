package org.odema.posnew.domain.service;

import org.odema.posnew.application.dto.InvoiceResponse;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InvoiceService {

    // ── Génération ───────────────────────────────────────
    InvoiceResponse generateInvoice(UUID orderId);
    InvoiceResponse reprintInvoice(UUID invoiceId);
    InvoiceResponse convertProformaToSale(UUID proformaId);

    // ── PDF ──────────────────────────────────────────────
    byte[] generateInvoicePdf(UUID invoiceId)                    throws IOException;
    byte[] getOrGenerateInvoicePdf(UUID orderId)                 throws IOException;
    byte[] regenerateInvoicePdf(UUID orderId)                    throws IOException;
    byte[] getOrGenerateProformaPdf(UUID orderId)                throws IOException;
    byte[] getOrGenerateCreditNotePdf(UUID orderId)              throws IOException;
    byte[] getOrGenerateDeliveryNotePdf(UUID orderId)            throws IOException;
    byte[] getOrGeneratePurchaseOrderPdf(UUID orderId)           throws IOException;
    byte[] getOrGenerateQuotePdf(UUID orderId)                   throws IOException;
    byte[] getOrGenerateCorrectedInvoicePdf(UUID orderId)        throws IOException;
    Resource getInvoicePdfResource(UUID orderId)                 throws IOException;
    String getInvoicePdfUrl(UUID invoiceId);

    // ── Lecture ──────────────────────────────────────────
    InvoiceResponse getInvoiceById(UUID invoiceId);
    InvoiceResponse getInvoiceByNumber(String invoiceNumber);
    InvoiceResponse getInvoiceByOrder(UUID orderId);
    List<InvoiceResponse> getInvoicesByCustomer(UUID customerId);
    List<InvoiceResponse> getInvoicesByStore(UUID storeId);
    List<InvoiceResponse> getInvoicesByStatus(String status);
    List<InvoiceResponse> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate);
    List<InvoiceResponse> getOverdueInvoices();

    // ── Mutations ────────────────────────────────────────
    InvoiceResponse updateInvoiceStatus(UUID invoiceId, String status);
    InvoiceResponse markInvoiceAsPaid(UUID invoiceId, String paymentMethod);
    InvoiceResponse cancelInvoice(UUID invoiceId);
    void sendInvoiceByEmail(UUID invoiceId, String email) throws Exception;

    // ── Statistiques ─────────────────────────────────────
    Double getTotalOutstandingAmount();
}