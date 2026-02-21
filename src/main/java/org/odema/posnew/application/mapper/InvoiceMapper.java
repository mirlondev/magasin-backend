package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.InvoiceResponse;
import org.odema.posnew.domain.model.Invoice;
import org.odema.posnew.domain.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InvoiceMapper {

    private final FileStorageService fileStorageService;

    @Value("${app.file.directories.invoices:invoices}")
    private String invoicesDirectory;

    public InvoiceMapper(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        if (invoice == null) return null;

        return new InvoiceResponse(
                invoice.getInvoiceId() != null ? invoice.getInvoiceId().toString() : null,
                invoice.getInvoiceNumber(),
                invoice.getInvoiceType(),
                invoice.getStatus(),
                invoice.getOrder() != null && invoice.getOrder().getOrderId() != null
                        ? invoice.getOrder().getOrderId().toString() : null,
                invoice.getOrder() != null ? invoice.getOrder().getOrderNumber() : null,
                invoice.getCustomer() != null && invoice.getCustomer().getCustomerId() != null
                        ? invoice.getCustomer().getCustomerId().toString() : null,
                invoice.getCustomer() != null ? invoice.getCustomer().getFullName() : null,
                invoice.getStore() != null && invoice.getStore().getStoreId() != null
                        ? invoice.getStore().getStoreId().toString() : null,
                invoice.getStore() != null ? invoice.getStore().getName() : null,
                invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().toLocalDate() : null,
                invoice.getPaymentDueDate() != null ? invoice.getPaymentDueDate().toLocalDate() : null,
                invoice.getValidityDays(),
                invoice.getSubtotal(),
                invoice.getTaxAmount(),
                invoice.getDiscountAmount(),
                invoice.getTotalAmount(),
                invoice.getAmountPaid(),
                invoice.getAmountDue(),
                invoice.getPaymentMethod(),
                getPdfUrl(invoice),
                invoice.getPrintCount(),
                invoice.getLastPrintedAt(),
                invoice.getConvertedToSale(),
                invoice.getConvertedAt(),
                invoice.getConvertedOrder() != null && invoice.getConvertedOrder().getOrderId() != null
                        ? invoice.getConvertedOrder().getOrderId().toString() : null,
                invoice.getNotes(),
                invoice.getIsActive(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }

    public List<InvoiceResponse> toResponseList(List<Invoice> invoices) {
        if (invoices == null) return List.of();
        return invoices.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private String getPdfUrl(Invoice invoice) {
        if (invoice.getPdfFilename() != null && fileStorageService != null) {
            return fileStorageService.getFileUrl(invoice.getPdfFilename(), invoicesDirectory);
        }
        return null;
    }
}