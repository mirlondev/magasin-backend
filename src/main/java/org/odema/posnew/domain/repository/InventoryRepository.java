package org.odema.posnew.domain.repository;

import org.odema.posnew.application.dto.response.InventorySummaryProjection;
import org.odema.posnew.domain.model.Inventory;
import org.odema.posnew.domain.model.enums.StockStatus;
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

    @Query("""
    SELECT (
        COUNT(i),
        SUM(CASE WHEN i.quantity <= i.reorderPoint AND i.quantity > 0 THEN 1 ELSE 0 END),
        SUM(CASE WHEN i.quantity <= 0 THEN 1 ELSE 0 END),
        SUM(CAST(i.quantity AS long)),
        SUM(CASE WHEN i.unitCost IS NOT NULL
                 THEN i.unitCost * CAST(i.quantity AS bigdecimal)
                 ELSE 0 END)
    )
    FROM Inventory i
    WHERE i.store.storeId = :storeId
    AND i.isActive = true
""")
    InventorySummaryProjection getSummaryByStore(@Param("storeId") UUID storeId);
}
