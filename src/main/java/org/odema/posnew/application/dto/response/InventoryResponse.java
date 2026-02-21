package org.odema.posnew.application.dto;

import org.odema.posnew.domain.model.enums.StockStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record InventoryResponse(
        UUID inventoryId,

        UUID productId,
        String productName,
        String productSku,

        UUID storeId,
        String storeName,
        String storeType,

        Integer quantity,
        BigDecimal unitCost,
        BigDecimal sellingPrice,
        BigDecimal totalValue,

        Integer reorderPoint,
        Integer maxStock,
        Integer minStock,

        StockStatus stockStatus,

        Boolean isLowStock,
        Boolean isOutOfStock,
        Boolean isOverStock,

        LocalDateTime lastRestocked,
        LocalDateTime nextRestockDate,

        String notes,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isActive
) {
}
