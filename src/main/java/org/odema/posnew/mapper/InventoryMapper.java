package org.odema.posnew.mapper;

import org.odema.posnew.dto.request.InventoryRequest;
import org.odema.posnew.dto.response.InventoryResponse;
import org.odema.posnew.entity.*;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class InventoryMapper {

    public Inventory toEntity(InventoryRequest request, Product product, Store store) {
        if (request == null) return null;

        Inventory inventory = Inventory.builder()
                .product(product)
                .store(store)
                .quantity(request.quantity())
                .unitCost(request.unitCost())
                .sellingPrice(request.sellingPrice())
                .reorderPoint(request.reorderPoint() != null ? request.reorderPoint() : 10)
                .maxStock(request.maxStock() != null ? request.maxStock() : 1000)
                .minStock(request.minStock() != null ? request.minStock() : 5)
                .notes(request.notes())
                .isActive(true)
                .build();

        // Mettre à jour le statut du stock
        inventory.updateStockStatus();

        return inventory;
    }

    public InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) return null;

        return new InventoryResponse(
                inventory.getInventoryId(),

                inventory.getProduct() != null ? inventory.getProduct().getProductId() : null,
                inventory.getProduct() != null ? inventory.getProduct().getName() : null,
                inventory.getProduct() != null ? inventory.getProduct().getSku() : null,

                inventory.getStore() != null ?
                        UUID.fromString(String.valueOf(inventory.getStore().getStoreId())) : null,
                inventory.getStore() != null ? inventory.getStore().getName() : null,
                inventory.getStore() != null ? inventory.getStore().getStoreType().name() : null,

                inventory.getQuantity(),
                inventory.getUnitCost(),
                inventory.getSellingPrice(),
                inventory.getTotalValue(),

                inventory.getReorderPoint(),
                inventory.getMaxStock(),
                inventory.getMinStock(),

                inventory.getStockStatus(),

                inventory.isLowStock(),
                inventory.isOutOfStock(),
                inventory.isOverStock(),

                inventory.getLastRestocked(),
                inventory.getNextRestockDate(),

                inventory.getNotes(),

                inventory.getCreatedAt(),
                inventory.getUpdatedAt(),
                inventory.getIsActive()
        );
    }
}
