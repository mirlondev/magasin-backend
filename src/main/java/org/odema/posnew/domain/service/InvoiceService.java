package org.odema.posnew.application.service;

import com.itextpdf.text.DocumentException;
import org.odema.posnew.application.dto.response.InvoiceResponse;
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
    byte[] generateInvoicePdf(UUID invoiceId)                    throws IOException, DocumentException;
    byte[] getOrGenerateInvoicePdf(UUID orderId)                 throws IOException, DocumentException;
    byte[] regenerateInvoicePdf(UUID orderId)                    throws IOException, DocumentException;
    byte[] getOrGenerateProformaPdf(UUID orderId)                throws IOException, DocumentException;
    byte[] getOrGenerateCreditNotePdf(UUID orderId)              throws IOException, DocumentException;
    byte[] getOrGenerateDeliveryNotePdf(UUID orderId)            throws IOException, DocumentException;
    byte[] getOrGeneratePurchaseOrderPdf(UUID orderId)           throws IOException, DocumentException;
    byte[] getOrGenerateQuotePdf(UUID orderId)                   throws IOException, DocumentException;
    byte[] getOrGenerateCorrectedInvoicePdf(UUID orderId)        throws IOException, DocumentException;
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