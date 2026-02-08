package org.odema.posnew.repository;

import org.odema.posnew.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    // Méthodes paginées
    Page<Product> findByCategory_CategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Méthodes non paginées (pour compatibilité)
    List<Product> findByCategory_CategoryId(UUID categoryId);

    List<Product> findByNameContainingIgnoreCase(String name);

    Optional<Product> findBySku(String sku);

    Optional<Product> findByBarcode(String barcode);

    @Query("""
        SELECT DISTINCT p
        FROM Product p
        JOIN p.inventories i
        WHERE i.isActive = true
        AND i.stockStatus = 'LOW_STOCK'
        """)
    List<Product> findLowStockProducts();

    @Query("""
        SELECT DISTINCT p
        FROM Product p
        JOIN p.inventories i
        WHERE i.isActive = true
        AND i.stockStatus = 'LOW_STOCK'
        """)
    Page<Product> findLowStockProducts(Pageable pageable);

    // Méthode paginée pour tous les produits actifs
    Page<Product> findByIsActiveTrue(Pageable pageable);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);
    @Query("""
        SELECT DISTINCT p
        FROM Product p
        WHERE p.isActive = true
        """)
    List<Product> findAllActiveProducts();
}