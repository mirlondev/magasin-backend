package org.odema.posnew.repository;

import jakarta.validation.constraints.NotNull;
import org.odema.posnew.entity.Inventory;
import org.odema.posnew.entity.enums.StockStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    // Trouver l'inventaire par produit et store (SANS Pageable - car unique)
    Optional<Inventory> findByProduct_ProductIdAndStore_StoreId(UUID productId, UUID storeId);

    // Trouver tous les inventaires d'un store (AVEC pagination)
    Page<Inventory> findByStore_StoreId(UUID storeId, Pageable pageable);
    List<Inventory> findByStore_StoreId(UUID storeId);

    // Trouver tous les inventaires d'un store (SANS pagination - version alternative)
//    List<Inventory> findByStore_StoreId(UUID storeId);

    // Trouver tous les inventaires d'un produit (dans tous les stores) - AVEC pagination
    Page<Inventory> findByProduct_ProductId(UUID productId, Pageable pageable);

    // Trouver tous les inventaires d'un produit (SANS pagination)
    List<Inventory> findByProduct_ProductId(UUID productId);

    // Trouver les inventaires par statut de stock - AVEC pagination
    Page<Inventory> findByStockStatus(StockStatus stockStatus, Pageable pageable);

    // Trouver les inventaires par statut de stock - SANS pagination
    List<Inventory> findByStockStatus(StockStatus stockStatus);
    Page<Inventory> getInventoryByStore(UUID storeId, Pageable pageable);
    // Trouver les inventaires en stock faible dans un store - AVEC pagination
    Page<Inventory> findByStore_StoreIdAndStockStatus(UUID storeId, StockStatus stockStatus, Pageable pageable);

    // Trouver les inventaires en stock faible dans un store - SANS pagination
    List<Inventory> findByStore_StoreIdAndStockStatus(UUID storeId, StockStatus stockStatus);

    // Obtenir la quantité totale d'un produit dans tous les stores
    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.product.productId = :productId AND i.isActive = true")
    Integer findTotalQuantityByProduct(@Param("productId") UUID productId);

    // Vérifier l'existence
    boolean existsByProduct_ProductIdAndStore_StoreId(UUID productId, UUID storeId);

    // Trouver les inventaires en stock faible par seuil - AVEC pagination
    @Query("SELECT i FROM Inventory i WHERE i.quantity <= :threshold AND i.isActive = true")
    Page<Inventory> findLowStockByThreshold(@Param("threshold") Integer threshold, Pageable pageable);

    // Trouver les inventaires en stock faible par seuil - SANS pagination
    @Query("SELECT i FROM Inventory i WHERE i.quantity <= :threshold AND i.isActive = true")
    List<Inventory> findLowStockByThreshold(@Param("threshold") Integer threshold);

    // Trouver par statut et actif - AVEC pagination
    Page<Inventory> findByStockStatusAndIsActiveTrue(StockStatus stockStatus, Pageable pageable);

    // Trouver par statut et actif - SANS pagination
    List<Inventory> findByStockStatusAndIsActiveTrue(StockStatus stockStatus);

    // Obtenir la valeur totale du stock par store
    @Query("SELECT SUM(i.quantity * i.unitCost) FROM Inventory i WHERE i.store.storeId = :storeId AND i.isActive = true")
    BigDecimal findTotalStockValueByStore(@Param("storeId") UUID storeId);

    // Trouver tous les inventaires actifs - AVEC pagination
    Page<Inventory> findByIsActiveTrue(Pageable pageable);
}