// ReceiptResponse.java - DTO plat sans références circulaires
package org.odema.posnew.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class ReceiptResponse {
    // IDs uniquement, pas d'objets complets
    private UUID orderId;
    private String orderNumber;

    // Données primitives uniquement
    private String customerName;
    private String customerPhone;
    private String cashierName;
    private String storeName;
    private String storeAddress;
    private String storePhone;

    // Liste d'items DTO, pas d'entités
    private List<ReceiptItemDto> items;

    // Totaux
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal totalPaid;
    private BigDecimal remainingAmount;
    private BigDecimal changeAmount;

    // Paiements simplifiés
    private Map<String, BigDecimal> paymentsByMethod; // "CASH" -> 5000
    private BigDecimal creditAmount;

    private String notes;
    private LocalDateTime createdAt;

    // DTO interne statique
    @Data
    @Builder
    public static class ReceiptItemDto {
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal finalPrice;
        private BigDecimal discountAmount;
    }
}