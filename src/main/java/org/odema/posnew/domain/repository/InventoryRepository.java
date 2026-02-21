package org.odema.posnew.application.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProduct_ProductIdAndStore_StoreId(UUID productId, UUID storeId);

    boolean existsByProduct_ProductIdAndStore_StoreId(UUID productId, UUID storeId);

    List<Inventory> findByStore_StoreId(UUID storeId);

    Page<Inventory> findByStore_StoreId(UUID storeId, Pageable pageable);

    List<Inventory> findByProduct_ProductId(UUID productId);

    Page<Inventory> findByProduct_ProductId(UUID productId, Pageable pageable);

    List<Inventory> findByIsActiveTrue();

    Page<Inventory> findByIsActiveTrue(Pageable pageable);

    List<Inventory> findByStockStatusAndIsActiveTrue(StockStatus status);

    Page<Inventory> findByStockStatusAndIsActiveTrue(StockStatus status, Pageable pageable);

    @Query("SELECT i FROM Inventory i WHERE i.isActive = true AND i.quantity <= :threshold")
    List<Inventory> findLowStockByThreshold(@Param("threshold") int threshold);

    @Query("SELECT i FROM Inventory i WHERE i.isActive = true AND i.quantity <= :threshold")
    Page<Inventory> findLowStockByThreshold(@Param("threshold") int threshold, Pageable pageable);

    @Query("SELECT SUM(i.quantity * i.unitCost) FROM Inventory i WHERE i.store.storeId = :storeId AND i.isActive = true")
    BigDecimal findTotalStockValueByStore(@Param("storeId") UUID storeId);

    @Query("SELECT i FROM Inventory i WHERE i.isActive = true AND i.product.productId = :productId " +
            "AND i.quantity > 0")
    List<Inventory> findAvailableInventoriesForProduct(@Param("productId") UUID productId);
}
