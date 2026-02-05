package org.odema.posnew.service;

import org.odema.posnew.dto.request.InventoryRequest;
import org.odema.posnew.dto.request.InventoryTransferRequest;
import org.odema.posnew.dto.request.InventoryUpdateRequest;
import org.odema.posnew.dto.response.InventoryResponse;
import org.odema.posnew.dto.response.InventorySummaryResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface InventoryService {
    InventoryResponse createInventory(InventoryRequest request);

    InventoryResponse getInventoryById(UUID inventoryId);

    InventoryResponse getInventoryByProductAndStore(UUID productId, UUID storeId);

    InventoryResponse updateInventory(UUID inventoryId, InventoryUpdateRequest request);

    void deleteInventory(UUID inventoryId);

    List<InventoryResponse> getAllInventory();

    List<InventoryResponse> getInventoryByStore(UUID storeId);

    List<InventoryResponse> getInventoryByProduct(UUID productId);

    List<InventoryResponse> getLowStockInventory(Integer threshold);

    List<InventoryResponse> getInventoryByStatus(String status);

    InventoryResponse updateStock(UUID inventoryId, Integer quantity, String operation);

    InventoryResponse transferStock(InventoryTransferRequest request);

    InventorySummaryResponse getInventorySummary(UUID storeId);

    BigDecimal getTotalStockValue(UUID storeId);

    void restockInventory(UUID inventoryId, Integer quantity, BigDecimal unitCost);

    void updateStockStatus(UUID inventoryId);
}
