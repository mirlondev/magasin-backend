package org.odema.posnew.service.impl;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.dto.request.ProductRequest;
import org.odema.posnew.dto.response.ProductResponse;
import org.odema.posnew.entity.Category;
import org.odema.posnew.entity.Product;
import org.odema.posnew.entity.User;
import org.odema.posnew.entity.enums.UserRole;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.exception.UnauthorizedException;
import org.odema.posnew.mapper.ProductMapper;
import org.odema.posnew.repository.CategoryRepository;
import org.odema.posnew.repository.ProductRepository;
import org.odema.posnew.service.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, User user) throws UnauthorizedException, NotFoundException {
        // Vérifier les permissions
        if (!hasProductManagementPermission(user)) {
            throw new UnauthorizedException("Permission insuffisante pour créer un produit");
        }

        // Vérifier l'unicité du SKU
        if (request.sku() != null && productRepository.existsBySku(request.sku())) {
            throw new BadRequestException("Un produit avec ce SKU existe déjà");
        }

        // Vérifier l'unicité du code-barres
        if (request.barcode() != null && productRepository.existsByBarcode(request.barcode())) {
            throw new BadRequestException("Un produit avec ce code-barres existe déjà");
        }

        // Récupérer la catégorie
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Catégorie non trouvée"));

        // Créer le produit
        Product product = productMapper.toEntity(request, category, null);
        Product savedProduct = productRepository.save(product);

        return productMapper.toResponse(savedProduct);
    }

    @Override
    public ProductResponse getProductById(UUID productId) throws NotFoundException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID productId, ProductRequest request) throws NotFoundException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

        // Mettre à jour les champs
        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null) product.setPrice(request.price());
//        if (request.quantity() != null) product.(request.quantity());
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("Catégorie non trouvée"));
            product.setCategory(category);
        }
        if (request.imageUrl() != null) product.setImageUrl(request.imageUrl());
        if (request.sku() != null) product.setSku(request.sku());
        if (request.barcode() != null) product.setBarcode(request.barcode());

        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID productId) throws NotFoundException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

        productRepository.delete(product);
    }


    @Override
    public List<ProductResponse> getAllProducts() throws UnauthorizedException{
        return productRepository.findAll().stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> getProductsByCategory(UUID categoryId) {
        List<Product> products = productRepository.findByCategory_CategoryId(categoryId);
        return products.stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> searchProducts(String keyword) {
        List<Product> products = productRepository.findByNameContainingIgnoreCase(keyword);
        return products.stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> getLowStockProducts(Integer threshold) {
       // int stockThreshold = threshold != null ? threshold : 10;
        List<Product> products = productRepository.findLowStockProducts();
        return products.stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductResponse updateStock(UUID productId, Integer quantity, String operation) throws NotFoundException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

//        switch (operation.toLowerCase()) {
//            case "add" -> product.increaseQuantity(quantity);
//            case "remove" -> product.decreaseQuantity(quantity);
//            case "set" -> product.setQuantity(quantity);
//            default -> throw new BadRequestException("Opération invalide: utilisez 'add', 'remove' ou 'set'");
//        }

        Product updatedProduct = productRepository.save(product);
        return productMapper.toResponse(updatedProduct);
    }

    private boolean hasProductManagementPermission(User user) {
        UserRole role = user.getUserRole();
        return role == UserRole.ADMIN ||
                role == UserRole.STORE_ADMIN ||
                role == UserRole.CASHIER;
    }
}