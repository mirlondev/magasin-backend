package org.odema.posnew.repository;

import org.odema.posnew.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

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


//    @Query("SELECT p FROM Product p WHERE p. = true ORDER BY p.createdAt DESC")
//    List<Product> findAllActiveProducts();

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);
}