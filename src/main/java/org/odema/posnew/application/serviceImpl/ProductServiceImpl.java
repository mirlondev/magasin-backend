package org.odema.posnew.application.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.odema.posnew.api.exception.BadRequestException;
import org.odema.posnew.api.exception.NotFoundException;
import org.odema.posnew.api.exception.UnauthorizedException;
import org.odema.posnew.application.dto.request.ProductRequest;
import org.odema.posnew.application.dto.response.ProductResponse;
import org.odema.posnew.application.mapper.ProductMapper;

import org.odema.posnew.domain.model.Category;
import org.odema.posnew.domain.model.Product;
import org.odema.posnew.domain.model.User;
import org.odema.posnew.domain.model.enums.UserRole;
import org.odema.posnew.domain.repository.CategoryRepository;
import org.odema.posnew.domain.repository.ProductRepository;
import org.odema.posnew.domain.service.ProductService;
import org.springframework.data.domain.Page;
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
        Product product = productMapper.toEntity(request, category);
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
    public void deleteProduct(UUID productId) throws NotFoundException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

        productRepository.delete(product);
    }

    // ============ MÉTHODES PAGINÉES ============

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findByIsActiveTrue(pageable)
                .map(productMapper::toResponse);
    }

    @Override
    public Page<ProductResponse> getProductsByCategory(UUID categoryId, Pageable pageable) {
        return productRepository.findByCategory_CategoryId(categoryId, pageable)
                .map(productMapper::toResponse);
    }

    @Override
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable)
                .map(productMapper::toResponse);
    }

    @Override
    public Page<ProductResponse> getLowStockProducts(Pageable pageable) {
        return productRepository.findLowStockProducts(pageable)
                .map(productMapper::toResponse);
    }

    // ============ MÉTHODES NON PAGINÉES ============

    @Override
    public List<ProductResponse> getAllProducts() {
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
    public List<ProductResponse> getLowStockProducts(Integer threshold){
        List<Product> products = productRepository.findLowStockProducts();
        return products.stream()
                .map(productMapper::toResponse)
                .toList();
    }



    private boolean hasProductManagementPermission(User user) {
        UserRole role = user.getUserRole();
        return role == UserRole.ADMIN ||
                role == UserRole.STORE_ADMIN ||
                role == UserRole.CASHIER;
    }


    @Override
    @Transactional
    public ProductResponse updateProduct(UUID productId, ProductRequest request)
            throws NotFoundException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

        if (request.name() != null)        product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("Catégorie non trouvée"));
            product.setCategory(category);
        }
        if (request.imageUrl() != null)  product.setImageUrl(request.imageUrl());
        if (request.sku() != null)       product.setSku(request.sku());
        if (request.barcode() != null)   product.setBarcode(request.barcode());

        // ✅ SUPPRIMÉ : request.price() → n'existe plus dans Product
        // Le prix se gère via StorePricingService.setProductPrice()

        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse updateStock(UUID productId, Integer quantity, String operation)
            throws NotFoundException {
        // ✅ SUPPRIMÉ : logique de stock ici - Product n'a pas de champ stock
        // Rediriger vers InventoryService
        throw new BadRequestException(
                "La mise à jour du stock se fait via InventoryService.updateStock(). " +
                        "Utilisez /api/inventory/{inventoryId}/stock"
        );
    }
}