package org.odema.posnew.service;

import org.odema.posnew.dto.request.ProductRequest;
import org.odema.posnew.dto.response.ProductResponse;
import org.odema.posnew.entity.User;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.exception.UnauthorizedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request, User user) throws UnauthorizedException, NotFoundException;
    ProductResponse getProductById(UUID productId) throws NotFoundException;
    ProductResponse updateProduct(UUID productId, ProductRequest request) throws NotFoundException;
    void deleteProduct(UUID productId) throws NotFoundException;

    Page<ProductResponse> getAllProducts(Pageable pageable);

    Page<ProductResponse> getProductsByCategory(UUID categoryId, Pageable pageable);

    Page<ProductResponse> searchProducts(String keyword, Pageable pageable);

    Page<ProductResponse> getLowStockProducts(Pageable pageable);

    List<ProductResponse> getAllProducts() throws UnauthorizedException;
    List<ProductResponse> getProductsByCategory(UUID categoryId);
    List<ProductResponse> searchProducts(String keyword);
    List<ProductResponse> getLowStockProducts(Integer threshold);

//    List<ProductResponse> getLowStockProducts();

    ProductResponse updateStock(UUID productId, Integer quantity, String operation) throws NotFoundException;
}