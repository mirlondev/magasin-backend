package org.odema.posnew.service;

import com.itextpdf.text.DocumentException;
import org.odema.posnew.dto.response.InvoiceResponse;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InvoiceService {
    InvoiceResponse generateInvoice(UUID orderId) throws IOException, DocumentException;

    InvoiceResponse getInvoiceById(UUID invoiceId);

    InvoiceResponse getInvoiceByNumber(String invoiceNumber);

    InvoiceResponse getInvoiceByOrder(UUID orderId);

    List<InvoiceResponse> getInvoicesByCustomer(UUID customerId);

    List<InvoiceResponse> getInvoicesByStore(UUID storeId);

    List<InvoiceResponse> getInvoicesByStatus(String status);

    List<InvoiceResponse> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate);

    byte[] generateInvoicePdf(UUID invoiceId) throws IOException, DocumentException;

    String getInvoicePdfUrl(UUID invoiceId);

    InvoiceResponse updateInvoiceStatus(UUID invoiceId, String status);

    InvoiceResponse markInvoiceAsPaid(UUID invoiceId, String paymentMethod);

    InvoiceResponse cancelInvoice(UUID invoiceId);

    void sendInvoiceByEmail(UUID invoiceId, String email) throws Exception;

    List<InvoiceResponse> getOverdueInvoices();

    Double getTotalOutstandingAmount();

    InvoiceResponse reprintInvoice(UUID invoiceId);

    InvoiceResponse convertProformaToSale(UUID proformaId);
    byte[] getOrGenerateInvoicePdf(UUID orderId) throws IOException, DocumentException;

    /**
     * Force PDF regeneration (deletes old file)
     */
    byte[] regenerateInvoicePdf(UUID orderId) throws IOException, DocumentException;

    /**
     * Get PDF as Resource for streaming
     */
    Resource getInvoicePdfResource(UUID orderId) throws IOException;

}
