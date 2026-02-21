package org.odema.posnew.domain.repository;

import org.odema.posnew.domain.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySku(String sku);

    Optional<Product> findByBarcode(String barcode);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    List<Product> findByCategory_CategoryId(UUID categoryId);

    Page<Product> findByCategory_CategoryId(UUID categoryId, Pageable pageable);

    List<Product> findByIsActiveTrue();

    Page<Product> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
            "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Product> findByNameContainingIgnoreCase(@Param("keyword") String keyword);

    Page<Product> findByNameContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p JOIN p.inventories i WHERE p.isActive = true " +
            "AND i.quantity <= i.reorderPoint GROUP BY p")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p JOIN p.inventories i WHERE p.isActive = true " +
            "AND i.quantity <= i.reorderPoint GROUP BY p")
    Page<Product> findLowStockProducts(Pageable pageable);

    @Query("SELECT p FROM Product p JOIN p.inventories i WHERE p.isActive = true " +
            "AND i.store.storeId = :storeId AND i.quantity > 0")
    List<Product> findAvailableProductsInStore(@Param("storeId") UUID storeId);
}
