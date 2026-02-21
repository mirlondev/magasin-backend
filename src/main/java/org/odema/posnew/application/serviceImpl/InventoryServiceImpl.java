package org.odema.posnew.application.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.api.exception.BusinessException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.application.dto.*;
import org.odema.posnew.application.dto.InventoryRequest;
import org.odema.posnew.application.dto.InventoryResponse;
import org.odema.posnew.application.dto.InventorySummaryResponse;
import org.odema.posnew.application.dto.InventoryTransferRequest;
import org.odema.posnew.application.dto.InventoryUpdateRequest;
import org.odema.posnew.application.dto.response.InventorySummaryProjection;
import org.odema.posnew.application.mapper.InventoryMapper;
import org.odema.posnew.design.event.LowStockEvent;
import org.odema.posnew.design.event.StockAdjustmentEvent;
import org.odema.posnew.design.event.StockTransferEvent;
import org.odema.posnew.domain.model.Inventory;
import org.odema.posnew.domain.model.Product;
import org.odema.posnew.domain.model.Store;
import org.odema.posnew.domain.model.enums.StockStatus;
import org.odema.posnew.domain.repository.InventoryRepository;
import org.odema.posnew.domain.repository.ProductRepository;
import org.odema.posnew.domain.repository.StoreRepository;
import org.odema.posnew.domain.service.InventoryService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final InventoryMapper inventoryMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public InventoryResponse createInventory(InventoryRequest request) {
        if (inventoryRepository.existsByProduct_ProductIdAndStore_StoreId(
                request.productId(), request.storeId())) {
            throw new BusinessException("Un inventaire existe déjà pour ce produit dans ce store");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        Inventory inventory = inventoryMapper.toEntity(request, product, store);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // Vérifier si stock bas après création
        checkAndPublishLowStockEvent(savedInventory);

        return inventoryMapper.toResponse(savedInventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryById(UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventaire non trouvé"));

        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductAndStore(UUID productId, UUID storeId) {
        Inventory inventory = inventoryRepository
                .findByProduct_ProductIdAndStore_StoreId(productId, storeId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventaire non trouvé pour ce produit dans ce store"));

        return inventoryMapper.toResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse updateInventory(UUID inventoryId, InventoryUpdateRequest request) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventaire non trouvé"));
        int oldQuantity = inventory.getQuantity();

        if (request.quantity() != null && request.operation() != null) {
            updateStockQuantity(inventory, request.quantity(), request.operation());
        } else if (request.quantity() != null) {
            inventory.setQuantity(request.quantity());
        }

        if (request.unitCost() != null) inventory.setUnitCost(request.unitCost());
        if (request.sellingPrice() != null) inventory.setSellingPrice(request.sellingPrice());
        if (request.reorderPoint() != null) inventory.setReorderPoint(request.reorderPoint());
        if (request.maxStock() != null) inventory.setMaxStock(request.maxStock());
        if (request.minStock() != null) inventory.setMinStock(request.minStock());
        if (request.stockStatus() != null) inventory.setStockStatus(request.stockStatus());
        if (request.nextRestockDate() != null) inventory.setNextRestockDate(request.nextRestockDate());
        if (request.notes() != null) inventory.setNotes(request.notes());

        if (request.quantity() != null || request.reorderPoint() != null || request.maxStock() != null) {
            inventory.updateStockStatus();
        }

        Inventory updatedInventory = inventoryRepository.save(inventory);

        // Publier événement d'ajustement si la quantité a changé
        if (oldQuantity != updatedInventory.getQuantity()) {
            publishStockAdjustmentEvent(updatedInventory, "UPDATE", Math.abs(updatedInventory.getQuantity() - oldQuantity), request.notes());
        }

        checkAndPublishLowStockEvent(updatedInventory);

        return inventoryMapper.toResponse(updatedInventory);
    }

    @Override
    @Transactional
    public void deleteInventory(UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventaire non trouvé"));

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

        // Vérifier si stock bas après mise à jour
        checkAndPublishLowStockEvent(updatedInventory);

        return inventoryMapper.toResponse(updatedInventory);
    }

    @Override
    @Transactional
    public InventoryResponse transferStock(InventoryTransferRequest request) {

        if (request.quantity() <= 0) {
            throw new BadRequestException(
                    "La quantité à transférer doit être strictement positive");
        }
        if (request.fromStoreId().equals(request.toStoreId())) {
            throw new BusinessException("Les stores source et destination doivent être différents");
        }

        Inventory sourceInventory = inventoryRepository
                .findByProduct_ProductIdAndStore_StoreId(
                        request.productId(), request.fromStoreId())
                .orElseThrow(() -> new NotFoundException("Inventaire source non trouvé"));

        if (sourceInventory.getQuantity() < request.quantity()) {
            throw new BusinessException(
                    "Stock insuffisant. Disponible: " + sourceInventory.getQuantity() +
                            ", Demandé: " + request.quantity());
        }

        Inventory targetInventory = inventoryRepository
                .findByProduct_ProductIdAndStore_StoreId(
                        request.productId(), request.toStoreId())
                .orElseGet(() -> {
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

        sourceInventory.decreaseQuantity(request.quantity());
        targetInventory.increaseQuantity(request.quantity());

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

        inventoryRepository.save(sourceInventory);
        Inventory savedTargetInventory = inventoryRepository.save(targetInventory);

        // Vérifier les stocks après transfert
        // Publier événement de transfert
        eventPublisher.publishEvent(new StockTransferEvent(
                this,
                sourceInventory.getProduct().getProductId(),
                sourceInventory.getProduct().getName(),
                sourceInventory.getStore().getStoreId(),
                sourceInventory.getStore().getName(),
                targetInventory.getStore().getStoreId(),
                targetInventory.getStore().getName(),
                request.quantity()
        ));

        checkAndPublishLowStockEvent(sourceInventory);
        checkAndPublishLowStockEvent(savedTargetInventory);

        return inventoryMapper.toResponse(savedTargetInventory);

    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getAllInventory(Pageable pageable) {
        return inventoryRepository.findByIsActiveTrue(pageable)
                .map(inventoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getInventoryByProduct(UUID productId, Pageable pageable) {
        return inventoryRepository.findByProduct_ProductId(productId, pageable)
                .map(inventoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getLowStockInventory(Integer threshold, Pageable pageable) {
        int actualThreshold = threshold != null ? threshold : 10;
        return inventoryRepository.findLowStockByThreshold(actualThreshold, pageable)
                .map(inventoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getInventoryByStatus(String status, Pageable pageable) {
        try {
            StockStatus stockStatus = StockStatus.valueOf(status.toUpperCase());
            return inventoryRepository.findByStockStatusAndIsActiveTrue(stockStatus, pageable)
                    .map(inventoryMapper::toResponse);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Statut de stock invalide: " + status);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .filter(Inventory::getIsActive)
                .map(inventoryMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getInventoryByStore(UUID storeId, Pageable pageable) {
        return inventoryRepository.findByStore_StoreId(storeId, pageable)
                .map(inventoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryByProduct(UUID productId) {
        return inventoryRepository.findByProduct_ProductId(productId).stream()
                .filter(Inventory::getIsActive)
                .map(inventoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getLowStockInventory(Integer threshold) {
        int actualThreshold = threshold != null ? threshold : 10;
        return inventoryRepository.findLowStockByThreshold(actualThreshold).stream()
                .map(inventoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryByStatus(String status) {
        try {
            StockStatus stockStatus = StockStatus.valueOf(status.toUpperCase());
            return inventoryRepository.findByStockStatusAndIsActiveTrue(stockStatus).stream()
                    .map(inventoryMapper::toResponse)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Statut de stock invalide: " + status);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InventorySummaryResponse getInventorySummary(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new NotFoundException("Store non trouvé"));

        // ✅ Une seule requête SQL au lieu de N+1
        InventorySummaryProjection proj =
                inventoryRepository.getSummaryByStore(storeId);

        return new InventorySummaryResponse(
                storeId,
                store.getName(),
                proj.totalProducts()    != null ? proj.totalProducts()    : 0L,
                proj.lowStockProducts() != null ? proj.lowStockProducts() : 0L,
                proj.outOfStockProducts() != null ? proj.outOfStockProducts() : 0L,
                proj.totalQuantity()    != null ? proj.totalQuantity().intValue() : 0,
                proj.totalValue()       != null ? proj.totalValue() : BigDecimal.ZERO,
                LocalDateTime.now()
        );
    }
    @Override
    @Transactional(readOnly = true)
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
            throw new BusinessException("La quantité de réapprovisionnement doit être positive");
        }

        inventory.increaseQuantity(quantity);
        if (unitCost != null) {
            inventory.setUnitCost(unitCost);
        }
        inventory.setLastRestocked(LocalDateTime.now());

        Inventory updated = inventoryRepository.save(inventory);
        publishStockAdjustmentEvent(updated, "RESTOCK", quantity, "Réapprovisionnement automatique ou manuel");

        // Vérifier si stock bas après réapprovisionnement (devrait être OK maintenant)
        checkAndPublishLowStockEvent(updated);
    }

    @Override
    @Transactional
    public void updateStockStatus(UUID inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new NotFoundException("Inventaire non trouvé"));

        inventory.updateStockStatus();
        inventoryRepository.save(inventory);
    }

    // Méthodes utilitaires privées
    private void updateStockQuantity(Inventory inventory, Integer quantity, String operation) {
        switch (operation.toLowerCase()) {
            case "add" -> inventory.increaseQuantity(quantity);
            case "remove" -> inventory.decreaseQuantity(quantity);
            case "set" -> inventory.setQuantity(quantity);
            default -> throw new BusinessException(
                    "Opération invalide. Utilisez 'add', 'remove' ou 'set'");
        }
    }

    private void checkAndPublishLowStockEvent(Inventory inventory) {
        if (inventory.isLowStock() || inventory.isOutOfStock()) {
            Product product = inventory.getProduct();
            Store store = inventory.getStore();

            // Correction : Ajout de 'this' comme source de l'événement
            LowStockEvent event = new LowStockEvent(
                    this,
                    product.getProductId(),
                    product.getName(),
                    store.getStoreId(),
                    store.getName(),
                    inventory.getQuantity(),
                    inventory.getReorderPoint() != null ? inventory.getReorderPoint() : 0
            );

            eventPublisher.publishEvent(event);
            log.warn("Low stock event published for product {} in store {}: {} units remaining",
                    product.getName(), store.getName(), inventory.getQuantity());
        }
    }

    private void publishStockAdjustmentEvent(Inventory inventory, String operation, int quantityChanged, String reason) {
        eventPublisher.publishEvent(new StockAdjustmentEvent(
                this,
                inventory.getInventoryId(), // Ou getInventoryId() selon votre entité
                inventory.getProduct().getProductId(),
                inventory.getProduct().getName(),
                inventory.getStore().getStoreId(),
                operation,
                quantityChanged,
                inventory.getQuantity(),
                reason != null ? reason : "Aucune raison fournie"
        ));
    }

}