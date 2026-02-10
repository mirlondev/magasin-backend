package org.odema.posnew.service;

import org.odema.posnew.dto.response.ReceiptResponse;

import java.util.UUID;

public interface ReceiptService {

    /**
     * Generate receipt data for an order
     */
    ReceiptResponse generateReceipt(UUID orderId);

    /**
     * Get receipt as plain text (for thermal printer)
     */
    String getReceiptText(UUID orderId);

    /**
     * Generate receipt as PDF
     */
    byte[] generateReceiptPdf(UUID orderId);

    /**
     * Generate thermal printer data in ESC/POS format
     */
    byte[] generateThermalPrinterData(UUID orderId);
}