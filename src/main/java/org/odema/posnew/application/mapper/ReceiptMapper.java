package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.response.ReceiptResponse;
import org.odema.posnew.domain.model.Receipt;
import org.odema.posnew.domain.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ReceiptMapper {

    private final FileStorageService fileStorageService;

    @Value("${app.file.directories.receipts:receipts}")
    private String receiptsDirectory;

    public ReceiptMapper(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public ReceiptResponse toResponse(Receipt receipt) {
        if (receipt == null) return null;

        return new ReceiptResponse(
                receipt.getReceiptId().toString(),
                receipt.getReceiptNumber(),
                receipt.getReceiptType(),
                receipt.getStatus(),
                receipt.getOrder() != null ? receipt.getOrder().getOrderId().toString() : null,
                receipt.getOrder() != null ? receipt.getOrder().getOrderNumber() : null,
                receipt.getShiftReport() != null ? receipt.getShiftReport().getShiftReportId().toString() : null,
                receipt.getCashier() != null ? receipt.getCashier().getUserId().toString() : null,
                receipt.getCashier() != null ? receipt.getCashier().getUsername() : null,
                receipt.getStore() != null ? receipt.getStore().getStoreId().toString() : null,
                receipt.getStore() != null ? receipt.getStore().getName() : null,
                receipt.getReceiptDate(),
                receipt.getTotalAmount(),
                receipt.getAmountPaid(),
                receipt.getChangeAmount(),
                receipt.getPaymentMethod() != null ? org.odema.posnew.domain.model.enums.PaymentMethod.valueOf(receipt.getPaymentMethod()) : null,
                getPdfUrl(receipt),
                receipt.getPrintCount(),
                receipt.getLastPrintedAt(),
                receipt.getNotes(),
                receipt.getIsActive(),
                receipt.getCreatedAt(),
                receipt.getUpdatedAt()
        );
    }

    public List<ReceiptResponse> toResponseList(List<Receipt> receipts) {
        if (receipts == null) return List.of();
        return receipts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private String getPdfUrl(Receipt receipt) {
        if (receipt.getPdfFilename() != null && !receipt.getPdfFilename().isBlank() && fileStorageService != null) {
            return fileStorageService.getFileUrl(receipt.getPdfFilename(), receiptsDirectory);
        }
        return null;
    }
}