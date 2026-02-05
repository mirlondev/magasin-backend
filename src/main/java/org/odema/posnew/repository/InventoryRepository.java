package org.odema.posnew.repository;

import jakarta.validation.constraints.NotNull;
import org.odema.posnew.entity.Inventory;
import org.odema.posnew.entity.enums.StockStatus;
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

    // Trouver l'inventaire par produit et store
    Optional<Inventory> findByProduct_ProductIdAndStore_StoreId(UUID productId, UUID storeId);

    // Trouver tous les inventaires d'un store
    List<Inventory> findByStore_StoreId(UUID storeId);

    // Trouver tous les inventaires d'un produit (dans tous les stores)
    List<Inventory> findByProduct_ProductId(UUID productId);

    // Trouver les inventaires par statut de stock
    List<Inventory> findByStockStatus(StockStatus stockStatus);

    // Trouver les inventaires en stock faible dans un store
    List<Inventory> findByStore_StoreIdAndStockStatus(UUID storeId, StockStatus stockStatus);

    // Trouver les inventaires en rupture de stock
    List<Inventory> findByStockStatusAndIsActiveTrue(StockStatus stockStatus);

    // Trouver les inventaires par seuil de quantité
    @Query("SELECT i FROM Inventory i WHERE i.quantity <= :threshold AND i.isActive = true")
    List<Inventory> findLowStockByThreshold(@Param("threshold") Integer threshold);

    // Vérifier si un inventaire existe pour un produit et un store
    boolean existsByProduct_ProductIdAndStore_StoreId(@NotNull(message = "Le produit est obligatoire") UUID productId, UUID storeId);

    // Obtenir la quantité totale d'un produit dans tous les stores
    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.product.productId = :productId AND i.isActive = true")
    Integer findTotalQuantityByProduct(@Param("productId") UUID productId);

    // Obtenir la valeur totale du stock dans un store
    @Query("SELECT SUM(i.quantity * i.unitCost) FROM Inventory i WHERE i.store.storeId = :storeId AND i.isActive = true AND i.unitCost IS NOT NULL")
    BigDecimal findTotalStockValueByStore(@Param("storeId") UUID storeId);
}
