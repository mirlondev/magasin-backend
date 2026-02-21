package org.odema.posnew.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreProductPriceRepository extends JpaRepository<StoreProductPrice, UUID> {

    List<StoreProductPrice> findByProduct_ProductIdAndStore_StoreId(UUID productId, UUID storeId);

    List<StoreProductPrice> findByStore_StoreIdAndIsActiveTrue(UUID storeId);

    List<StoreProductPrice> findByProduct_ProductId(UUID productId);

    @Query("SELECT spp FROM StoreProductPrice spp WHERE spp.product.productId = :productId " +
            "AND spp.store.storeId = :storeId AND spp.isActive = true " +
            "AND spp.effectiveDate <= :dateTime AND (spp.endDate IS NULL OR spp.endDate >= :dateTime) " +
            "ORDER BY spp.effectiveDate DESC")
    Optional<StoreProductPrice> findActivePriceForProductAtDate(
            @Param("productId") UUID productId,
            @Param("storeId") UUID storeId,
            @Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT spp FROM StoreProductPrice spp WHERE spp.product.productId = :productId " +
            "AND spp.store.storeId = :storeId AND spp.isActive = true " +
            "AND spp.effectiveDate <= CURRENT_TIMESTAMP AND (spp.endDate IS NULL OR spp.endDate >= CURRENT_TIMESTAMP) " +
            "ORDER BY spp.effectiveDate DESC")
    Optional<StoreProductPrice> findCurrentActivePrice(
            @Param("productId") UUID productId,
            @Param("storeId") UUID storeId);

    List<StoreProductPrice> findByEndDateBeforeAndIsActiveTrue(LocalDateTime dateTime);

    boolean existsByProduct_ProductIdAndStore_StoreIdAndEffectiveDate(
            UUID productId, UUID storeId, LocalDateTime effectiveDate);
}
