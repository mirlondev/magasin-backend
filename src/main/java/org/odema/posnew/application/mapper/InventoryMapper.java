package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.InventoryRequest;
import org.odema.posnew.application.dto.InventoryResponse;
import org.odema.posnew.domain.model.Inventory;
import org.odema.posnew.domain.model.Product;
import org.odema.posnew.domain.model.Store;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InventoryMapper {

    public Inventory toEntity(InventoryRequest request, Product product, Store store) {
        if (request == null) return null;

        return Inventory.builder()
                .product(product)
                .store(store)
                .quantity(request.quantity() != null ? request.quantity() : 0)
                .unitCost(request.unitCost())
                .sellingPrice(request.sellingPrice())
                .reorderPoint(request.reorderPoint() != null ? request.reorderPoint() : 10)
                .maxStock(request.maxStock() != null ? request.maxStock() : 1000)
                .minStock(request.minStock() != null ? request.minStock() : 5)
                .notes(request.notes())
                .isActive(true)
                .build();
    }

    public InventoryResponse toResponse(Inventory inventory) {
        if (inventory == null) return null;

        return new InventoryResponse(
                inventory.getInventoryId(),
                inventory.getProduct() != null ? inventory.getProduct().getProductId() : null,
                inventory.getProduct() != null ? inventory.getProduct().getName() : null,
                inventory.getProduct() != null ? inventory.getProduct().getSku() : null,
                inventory.getStore() != null ? inventory.getStore().getStoreId() : null,
                inventory.getStore() != null ? inventory.getStore().getName() : null,
                inventory.getStore() != null && inventory.getStore().getStoreType() != null
                        ? inventory.getStore().getStoreType().name() : null,
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

    public List<InventoryResponse> toResponseList(List<Inventory> inventories) {
        if (inventories == null) return List.of();
        return inventories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}