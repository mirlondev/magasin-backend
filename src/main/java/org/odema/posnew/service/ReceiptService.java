package org.odema.posnew.service;

import org.odema.posnew.dto.response.ReceiptResponse;

import java.util.UUID;

public interface ReceiptService {
    ReceiptResponse generateReceipt(UUID orderId);

    byte[] generateReceiptPdf(UUID orderId);

    String getReceiptText(UUID orderId);

}
