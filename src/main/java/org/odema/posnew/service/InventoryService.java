package org.odema.posnew.service;

import org.odema.posnew.dto.request.InventoryRequest;
import org.odema.posnew.dto.request.InventoryTransferRequest;
import org.odema.posnew.dto.request.InventoryUpdateRequest;
import org.odema.posnew.dto.response.InventoryResponse;
import org.odema.posnew.dto.response.InventorySummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public interface InventoryService {
    InventoryResponse createInventory(InventoryRequest request);

    InventoryResponse getInventoryById(UUID inventoryId);

    // Supprime cette méthode en double
    // InventoryResponse getInventoryByProductAndStore(UUID productId, UUID storeId, Pageable pageable);

    InventoryResponse getInventoryByProductAndStore(UUID productId, UUID storeId);

    InventoryResponse updateInventory(UUID inventoryId, InventoryUpdateRequest request);

    void deleteInventory(UUID inventoryId);

    // Méthode paginée principale
    Page<InventoryResponse> getInventory(UUID storeId, Pageable pageable);

    // Méthodes paginées pour les différentes requêtes
    Page<InventoryResponse> getAllInventory(Pageable pageable);

    Page<InventoryResponse> getInventoryByStore(UUID storeId, Pageable pageable);

    Page<InventoryResponse> getInventoryByProduct(UUID productId, Pageable pageable);

    Page<InventoryResponse> getLowStockInventory(Integer threshold, Pageable pageable);

    Page<InventoryResponse> getInventoryByStatus(String status, Pageable pageable);

    // Méthodes non-paginées (pour compatibilité)
    List<InventoryResponse> getAllInventory();


    //List<InventoryResponse> getInventoryByProduct(UUID productId);

   // List<InventoryResponse> getLowStockInventory(Integer threshold);

   // List<InventoryResponse> getInventoryByStatus(String status);

    InventoryResponse updateStock(UUID inventoryId, Integer quantity, String operation);

    InventoryResponse transferStock(InventoryTransferRequest request);

    List<InventoryResponse> getInventoryByProduct(UUID productId);

    List<InventoryResponse> getLowStockInventory(Integer threshold);

    List<InventoryResponse> getInventoryByStatus(String status);

    InventorySummaryResponse getInventorySummary(UUID storeId);

   // Page<InventorySummaryResponse> getInventorySummary(UUID storeId, Pageable pageable);

    BigDecimal getTotalStockValue(UUID storeId);

    void restockInventory(UUID inventoryId, Integer quantity, BigDecimal unitCost);

    void updateStockStatus(UUID inventoryId);
}