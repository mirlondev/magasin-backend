package org.odema.posnew.mapper;

import org.odema.posnew.dto.response.InvoiceResponse;
import org.odema.posnew.dto.response.ReceiptResponse;
import org.odema.posnew.entity.Invoice;
import org.odema.posnew.entity.Receipt;
import org.odema.posnew.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReceiptMapper {

    @Value("${app.file.directories.receipts:receipts}")
    private String receiptsDirectory;

    private final FileStorageService fileStorageService;

    public ReceiptMapper(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public ReceiptResponse toResponse(Receipt receipt) {
        String pdfUrl = (receipt.getPdfFilename() != null && !receipt.getPdfFilename().isBlank())
                ? fileStorageService.getFileUrl(receipt.getPdfFilename(), receiptsDirectory)
                : null;

        return new ReceiptResponse(
                receipt.getReceiptId().toString(),
                receipt.getReceiptNumber(),
                receipt.getReceiptType(),
                receipt.getStatus(),
                receipt.getOrder() != null ? receipt.getOrder().getOrderId().toString() : null,
                receipt.getOrder() != null ? receipt.getOrder().getOrderNumber()        : null,
                receipt.getShiftReport() != null
                        ? receipt.getShiftReport().getShiftReportId().toString() : null,
                receipt.getCashier().getUserId().toString(),
                receipt.getCashier().getUsername(),
                receipt.getStore().getStoreId().toString(),
                receipt.getStore().getName(),
                receipt.getReceiptDate(),
                receipt.getTotalAmount(),
                receipt.getAmountPaid(),
                receipt.getChangeAmount(),
                receipt.getPaymentMethod(),
                pdfUrl,
                receipt.getPrintCount(),
                receipt.getLastPrintedAt(),
                receipt.getNotes(),
                receipt.getIsActive(),
                receipt.getCreatedAt(),
                receipt.getUpdatedAt()
        );
    }
}



