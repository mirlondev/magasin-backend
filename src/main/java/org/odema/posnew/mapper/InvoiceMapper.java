package org.odema.posnew.mapper;

import org.odema.posnew.entity.Invoice;
import org.odema.posnew.dto.response.InvoiceResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InvoiceMapper {

    public InvoiceResponse toResponse(Invoice invoice) {
        if (invoice == null) return null;
        UUID customerId = null;
        String customerName = "N/A";
        String customerEmail = "N/A";
        String customerPhone = "N/A";

        if (invoice.getCustomer() != null) {
            customerId = invoice.getCustomer().getCustomerId();
            customerName = invoice.getCustomer().getFullName();
            customerEmail = invoice.getCustomer().getEmail();
            customerPhone = invoice.getCustomer().getPhone();
        }

        return new InvoiceResponse(
                invoice.getInvoiceId(),
                invoice.getInvoiceNumber(),

                invoice.getOrder() != null ? invoice.getOrder().getOrderId() : null,
                invoice.getOrder() != null ? invoice.getOrder().getOrderNumber() : null,

                customerId,
                customerName,
                customerEmail,
                customerPhone,


                invoice.getStore() != null ? invoice.getStore().getStoreId() : null,
                invoice.getStore() != null ? invoice.getStore().getName() : null,

                invoice.getSubtotal(),
                invoice.getTaxAmount(),
                invoice.getDiscountAmount(),
                invoice.getTotalAmount(),
                invoice.getAmountPaid(),
                invoice.getAmountDue(),

                invoice.getStatus(),
                invoice.getPaymentMethod(),

                invoice.getInvoiceDate(),
                invoice.getPaymentDueDate(),

                invoice.getPdfFilename(),
                invoice.getPdfPath() != null ?
                        "/api/files/view/invoices/" + invoice.getPdfFilename() : null,

                invoice.getNotes(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt(),
                invoice.getIsActive(),
                invoice.isPaid(),
                invoice.isOverdue()
        );
    }
}
