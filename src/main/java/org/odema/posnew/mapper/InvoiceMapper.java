package  org.odema.posnew.mapper;

import org.odema.posnew.dto.response.InvoiceResponse;
import org.odema.posnew.entity.Invoice;
import org.odema.posnew.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InvoiceMapper {

    @Value("${app.file.directories.invoices:invoices}")
    private String invoicesDirectory;

    private final FileStorageService fileStorageService;

    public InvoiceMapper(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public InvoiceResponse toResponse(Invoice invoice) {
        String pdfUrl = invoice.getPdfFilename() != null
                ? fileStorageService.getFileUrl(invoice.getPdfFilename(), invoicesDirectory)
                : null;

        return new InvoiceResponse(
                invoice.getInvoiceId().toString(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceType(),
                invoice.getStatus(),
                invoice.getOrder().getOrderId().toString(),
                invoice.getOrder().getOrderNumber(),
                invoice.getCustomer() != null ? invoice.getCustomer().getCustomerId().toString() : null,
                invoice.getCustomer() != null ? invoice.getCustomer().getFullName() : null,
                invoice.getStore().getStoreId().toString(),
                invoice.getStore().getName(),
                invoice.getInvoiceDate(),
                invoice.getPaymentDueDate(),
                invoice.getValidityDays(),
                invoice.getSubtotal(),
                invoice.getTaxAmount(),
                invoice.getDiscountAmount(),
                invoice.getTotalAmount(),
                invoice.getAmountPaid(),
                invoice.getAmountDue(),
                invoice.getPaymentMethod(),
                pdfUrl,
                invoice.getPrintCount(),
                invoice.getLastPrintedAt(),
                invoice.getConvertedToSale(),
                invoice.getConvertedAt(),
                invoice.getConvertedOrder() != null
                        ? invoice.getConvertedOrder().getOrderId().toString()
                        : null,
                invoice.getNotes(),
                invoice.getIsActive(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }
}