package org.odema.posnew.domain.repository;

import org.odema.posnew.domain.model.StoreProductPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreProductPriceRepository extends JpaRepository<StoreProductPrice, UUID> {

    List<StoreProductPrice> findByProduct_ProductIdAndStore_StoreId(UUID productId, UUID storeId);

    List<StoreProductPrice> findByStore_StoreIdAndIsActiveTrue(UUID storeId);

    List<StoreProductPrice> findByProduct_ProductId(UUID productId);

    // ✅ CORRECTION: Retourne Optional<StoreProductPrice> au lieu de ScopedValue
    @Query("SELECT spp FROM StoreProductPrice spp WHERE spp.product.productId = :productId " +
            "AND spp.store.storeId = :storeId AND spp.isActive = true " +
            "ORDER BY spp.effectiveDate DESC")
    Optional<StoreProductPrice> findActivePriceForProductAndStore(
            @Param("productId") UUID productId,
            @Param("storeId") UUID storeId);

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

    // ✅ Trouve les prix avec remises qui chevauchent une période
    @Query("SELECT spp FROM StoreProductPrice spp WHERE spp.product.productId = :productId " +
            "AND spp.store.storeId = :storeId AND spp.isActive = true " +
            "AND ((spp.effectiveDate <= :endDate AND spp.endDate >= :startDate) " +
            "OR (spp.endDate IS NULL AND spp.effectiveDate <= :endDate))")
    List<StoreProductPrice> findOverlappingPrices(
            @Param("productId") UUID productId,
            @Param("storeId") UUID storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ✅ Historique des prix (actifs et inactifs)
    @Query("SELECT spp FROM StoreProductPrice spp WHERE spp.product.productId = :productId " +
            "AND spp.store.storeId = :storeId ORDER BY spp.effectiveDate DESC")
    List<StoreProductPrice> findPriceHistory(
            @Param("productId") UUID productId,
            @Param("storeId") UUID storeId);

    // ✅ Prix actifs pour un store
    @Query("SELECT spp FROM StoreProductPrice spp WHERE spp.store.storeId = :storeId " +
            "AND spp.isActive = true AND spp.effectiveDate <= CURRENT_TIMESTAMP " +
            "AND (spp.endDate IS NULL OR spp.endDate >= CURRENT_TIMESTAMP)")
    List<StoreProductPrice> findActivePricesForStore(@Param("storeId") UUID storeId);

    List<StoreProductPrice> findByEndDateBeforeAndIsActiveTrue(LocalDateTime dateTime);

    boolean existsByProduct_ProductIdAndStore_StoreIdAndEffectiveDate(
            UUID productId, UUID storeId, LocalDateTime effectiveDate);
}