package org.odema.posnew.dto.response;

import org.odema.posnew.entity.enums.OrderStatus;
import org.odema.posnew.entity.enums.PaymentMethod;
import org.odema.posnew.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/*public record OrderResponse(
        UUID orderId,
        String orderNumber,

        UUID customerId,
        String customerName,
        String customerEmail,
        String customerPhone,

        UUID cashierId,
        String cashierName,

        UUID storeId,
        String storeName,

        List<OrderItemResponse> items,

        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal amountPaid,
        BigDecimal changeAmount,

        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,

        Boolean isTaxable,
        BigDecimal taxRate,

        String notes,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,

        Integer itemCount,
        Boolean canBeRefunded
) {
}*/

/**
 * Version mise à jour avec informations de paiement détaillées
 */
public record OrderResponse(
        // Identifiants
        UUID orderId,
        String orderNumber,

        // Client
        UUID customerId,
        String customerName,
        String customerEmail,
        String customerPhone,

        // Caissier
        UUID cashierId,
        String cashierName,

        // Store
        UUID storeId,
        String storeName,

        // Articles
        List<OrderItemResponse> items,

        // Montants
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,

        // ANCIEN CHAMP - Deprecated mais gardé pour compatibilité
        @Deprecated
        BigDecimal amountPaid,

        BigDecimal changeAmount,

        // NOUVEAUX CHAMPS PAIEMENT
        // Montant réellement payé (cash, mobile, card)
        // Montant en crédit
        // Montant restant à payer
        // Liste des paiements

        // Statuts
        OrderStatus status,

        @Deprecated
        PaymentMethod paymentMethod, // Deprecated - Maintenant dans Payment

        PaymentStatus paymentStatus,

        // Options
        Boolean isTaxable,
        BigDecimal taxRate,

        // Notes
        org.odema.posnew.entity.enums.OrderType orderType, String notes,

        // Dates
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt,

        // Compteurs
        Integer itemCount,
        // Nouveau
        Boolean canBeRefunded
) {
}