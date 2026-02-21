package org.odema.posnew.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.application.dto.request.InventoryRequest;
import org.odema.posnew.application.dto.request.InventoryTransferRequest;
import org.odema.posnew.application.dto.request.InventoryUpdateRequest;
import org.odema.posnew.application.dto.response.InventoryResponse;
import org.odema.posnew.application.dto.response.InventorySummaryResponse;
import org.odema.posnew.domain.enums_old.StockStatus;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.api.exception.ResourceNotFoundException;
import org.odema.posnew.application.mapper.InventoryMapper;
import org.odema.posnew.repository.InventoryRepository;
import org.odema.posnew.repository.ProductRepository;
import org.odema.posnew.repository.StoreRepository;
import org.odema.posnew.application.service.InventoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional
    public InventoryResponse createInventory(InventoryRequest request) {
        // Vérifier si l'inventaire existe déjà pour ce produit et store
        if (inventoryRepository.existsByProduct_ProductIdAndStore_StoreId(
                request.productId(), request.storeId())) {
            throw new BadRequestException("Un inventaire existe déjà pour ce produit dans ce store");
        }

        // Récupérer le produit
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé"));

        // Récupérer le store
        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Store non trouvé"));

        // Créer l'inventaire
        Inventory inventory = inventoryMapper.toEntity(request, product, store);
        Inventory savedInventory = inventoryRepository.save(inventory);

        return inventoryMapper.toResponse(savedInventory);
    }

    @Override
    public InventoryResponse getInventoryById(UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventaire non trouvé"));

        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryResponse getInventoryByProductAndStore(UUID productId, UUID storeId) {
        Inventory inventory = inventoryRepository
                .findByProduct_ProductIdAndStore_StoreId(productId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventaire non trouvé pour ce produit dans ce store"));

        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse updateInventory(UUID inventoryId, InventoryUpdateRequest request) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventaire non trouvé"));

        // Mettre à jour la quantité si spécifiée
        if (request.quantity() != null && request.operation() != null) {
            updateStockQuantity(inventory, request.quantity(), request.operation());
        } else if (request.quantity() != null) {
            inventory.setQuantity(request.quantity());
        }

        // Mettre à jour les autres champs
        if (request.unitCost() != null) inventory.setUnitCost(request.unitCost());
        if (request.sellingPrice() != null) inventory.setSellingPrice(request.sellingPrice());
        if (request.reorderPoint() != null) inventory.setReorderPoint(request.reorderPoint());
        if (request.maxStock() != null) inventory.setMaxStock(request.maxStock());
        if (request.minStock() != null) inventory.setMinStock(request.minStock());
        if (request.stockStatus() != null) inventory.setStockStatus(request.stockStatus());
        if (request.nextRestockDate() != null) inventory.setNextRestockDate(request.nextRestockDate());
        if (request.notes() != null) inventory.setNotes(request.notes());

        // Mettre à jour le statut si nécessaire
        if (request.quantity() != null || request.reorderPoint() != null || request.maxStock() != null) {
            inventory.updateStockStatus();
        }

        Inventory updatedInventory = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(updatedInventory);
    }

    @Override
    @Transactional
    public void deleteInventory(UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventaire non trouvé"));

        // Soft delete
        inventory.setIsActive(false);
        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse updateStock(UUID inventoryId, Integer quantity, String operation) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventaire non trouvé"));

        updateStockQuantity(inventory, quantity, operation);

        Inventory updatedInventory = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(updatedInventory);
    }

    @Override
    @Transactional
    public InventoryResponse transferStock(InventoryTransferRequest request) {
        // Vérifier que les stores sont différents
        if (request.fromStoreId().equals(request.toStoreId())) {
            throw new BadRequestException("Les stores source et destination doivent être différents");
        }

        // Récupérer l'inventaire source
        Inventory sourceInventory = inventoryRepository
                .findByProduct_ProductIdAndStore_StoreId(
                        request.productId(), request.fromStoreId())
                .orElseThrow(() -> new NotFoundException(
                        "Inventaire source non trouvé"));

        // Vérifier le stock disponible
        if (sourceInventory.getQuantity() < request.quantity()) {
            throw new BadRequestException(
                    "Stock insuffisant. Disponible: " + sourceInventory.getQuantity() +
                            ", Demandé: " + request.quantity());
        }

        // Récupérer ou créer l'inventaire destination
        Inventory targetInventory = inventoryRepository
                .findByProduct_ProductIdAndStore_StoreId(
                        request.productId(), request.toStoreId())
                .orElseGet(() -> {
                    // Créer un nouvel inventaire si inexistant
                    Product product = sourceInventory.getProduct();
                    Store targetStore = storeRepository.findById(request.toStoreId())
                            .orElseThrow(() -> new NotFoundException("Store destination non trouvé"));

                    return Inventory.builder()
                            .product(product)
                            .store(targetStore)
                            .quantity(0)
                            .unitCost(sourceInventory.getUnitCost())
                            .sellingPrice(sourceInventory.getSellingPrice())
                            .reorderPoint(sourceInventory.getReorderPoint())
                            .maxStock(sourceInventory.getMaxStock())
                            .minStock(sourceInventory.getMinStock())
                            .isActive(true)
                            .build();
                });

        // Effectuer le transfert
        sourceInventory.decreaseQuantity(request.quantity());
        targetInventory.increaseQuantity(request.quantity());

        // Ajouter des notes
        String transferNote = String.format(
                "Transfert de %d unités depuis %s. Notes: %s",
                request.quantity(),
                sourceInventory.getStore().getName(),
                request.notes() != null ? request.notes() : ""
        );

        sourceInventory.setNotes(
                (sourceInventory.getNotes() != null ? sourceInventory.getNotes() + "\n" : "") +
                        "Transfert sortant: " + transferNote
        );

        targetInventory.setNotes(
                (targetInventory.getNotes() != null ? targetInventory.getNotes() + "\n" : "") +
                        "Transfert entrant: " + transferNote
        );

        // Sauvegarder
        inventoryRepository.save(sourceInventory);
        Inventory savedTargetInventory = inventoryRepository.save(targetInventory);

        return inventoryMapper.toResponse(savedTargetInventory);
    }

    // ============ MÉTHODES PAGINÉES ============

    @Override
    public Page<InventoryResponse> getInventory(UUID storeId, Pageable pageable) {
        Page<Inventory> page;

        if (storeId != null) {
            page = inventoryRepository.findByStore_StoreId(storeId, pageable);
        } else {
            page = inventoryRepository.findByIsActiveTrue(pageable);
        }

        return page.map(inventoryMapper::toResponse);
    }

    @Override
    public Page<InventoryResponse> getAllInventory(Pageable pageable) {
        return inventoryRepository.findByIsActiveTrue(pageable)
                .map(inventoryMapper::toResponse);
    }



    @Override
    public Page<InventoryResponse> getInventoryByProduct(UUID productId, Pageable pageable) {
        return inventoryRepository.findByProduct_ProductId(productId, pageable)
                .map(inventoryMapper::toResponse);
    }

    @Override
    public Page<InventoryResponse> getLowStockInventory(Integer threshold, Pageable pageable) {
        int actualThreshold = threshold != null ? threshold : 10;
        return inventoryRepository.findLowStockByThreshold(actualThreshold, pageable)
                .map(inventoryMapper::toResponse);
    }

    @Override
    public Page<InventoryResponse> getInventoryByStatus(String status, Pageable pageable) {
        try {
            StockStatus stockStatus = StockStatus.valueOf(status.toUpperCase());
            return inventoryRepository.findByStockStatusAndIsActiveTrue(stockStatus, pageable)
                    .map(inventoryMapper::toResponse);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut de stock invalide: " + status);
        }
    }

    // ============ MÉTHODES NON PAGINÉES (pour compatibilité) ============

    @Override
    public List<InventoryResponse> getAllInventory() {
        try{
        return inventoryRepository.findAll().stream()
                .filter(Inventory::getIsActive)
                .map(inventoryMapper::toResponse).toList();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("pas d'inventaire disponible");
        }
        }

    @Override
    public Page<InventoryResponse> getInventoryByStore(UUID storeId, Pageable pageable) {
        try{
            return inventoryRepository.findByStore_StoreId(storeId, pageable)
                    .map(inventoryMapper::toResponse);
        } catch (IllegalArgumentException e) {
           throw new NotFoundException("Inventaire  non disponible dans ce Magasin");
        }

    }

    @Override
    public List<InventoryResponse> getInventoryByProduct(UUID productId) {
        return inventoryRepository.findByProduct_ProductId(productId).stream()
                .filter(Inventory::getIsActive)
                .map(inventoryMapper::toResponse)
                .toList();
    }

    @Override
    public List<InventoryResponse> getLowStockInventory(Integer threshold) {
        int actualThreshold = threshold != null ? threshold : 10;
        return inventoryRepository.findLowStockByThreshold(actualThreshold).stream()
                .map(inventoryMapper::toResponse)
                .toList();
    }

    @Override
    public List<InventoryResponse> getInventoryByStatus(String status) {
        try {
            StockStatus stockStatus = StockStatus.valueOf(status.toUpperCase());
            return inventoryRepository.findByStockStatusAndIsActiveTrue(stockStatus).stream()
                    .map(inventoryMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Statut de stock invalide: " + status);
        }
    }

    @Override
    public InventorySummaryResponse getInventorySummary(UUID storeId ) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        List<Inventory> inventories = inventoryRepository.findByStore_StoreId(storeId);

        long totalProducts = inventories.stream()
                .filter(Inventory::getIsActive)
                .count();

        long lowStockProducts = inventories.stream()
                .filter(Inventory::getIsActive)
                .filter(Inventory::isLowStock)
                .count();

        long outOfStockProducts = inventories.stream()
                .filter(Inventory::getIsActive)
                .filter(Inventory::isOutOfStock)
                .count();

        BigDecimal totalValue = inventories.stream()
                .filter(Inventory::getIsActive)
                .map(Inventory::getTotalValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalQuantity = inventories.stream()
                .filter(Inventory::getIsActive)
                .mapToInt(Inventory::getQuantity)
                .sum();

        return new InventorySummaryResponse(
                storeId,
                store.getName(),
                totalProducts,
                lowStockProducts,
                outOfStockProducts,
                totalQuantity,
                totalValue,
                LocalDateTime.now()
        );
    }

    @Override
    public BigDecimal getTotalStockValue(UUID storeId) {
        BigDecimal value = inventoryRepository.findTotalStockValueByStore(storeId);
        return value != null ? value : BigDecimal.ZERO;
    }

    @Override
    @Transactional
    public void restockInventory(UUID inventoryId, Integer quantity, BigDecimal unitCost) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventaire non trouvé"));

        if (quantity <= 0) {
            throw new BadRequestException("La quantité de réapprovisionnement doit être positive");
        }

        inventory.increaseQuantity(quantity);
        if (unitCost != null) {
            inventory.setUnitCost(unitCost);
        }
        inventory.setLastRestocked(LocalDateTime.now());

        inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void updateStockStatus(UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventaire non trouvé"));

        inventory.updateStockStatus();
        inventoryRepository.save(inventory);
    }

    private void updateStockQuantity(Inventory inventory, Integer quantity, String operation) {
        switch (operation.toLowerCase()) {
            case "add" -> inventory.increaseQuantity(quantity);
            case "remove" -> inventory.decreaseQuantity(quantity);
            case "set" -> inventory.setQuantity(quantity);
            default -> throw new BadRequestException(
                    "Opération invalide. Utilisez 'add', 'remove' ou 'set'");
        }
    }
}