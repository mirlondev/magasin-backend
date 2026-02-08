package org.odema.posnew.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.entity.Product;
import org.odema.posnew.exception.BadRequestException;
import org.odema.posnew.exception.NotFoundException;
import org.odema.posnew.repository.ProductRepository;
import org.odema.posnew.service.FileStorageService;
import org.odema.posnew.service.ProductImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageServiceImpl implements ProductImageService {

    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.file.directories.products:products}")
    private String productsDirectory;

    @Value("${app.file.max-image-size:5242880}") // 5MB
    private long maxImageSize;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    @Transactional
    public String uploadProductImage(UUID productId, MultipartFile imageFile) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

        // Valider l'image
        if (!validateProductImage(imageFile)) {
            throw new BadRequestException("Image invalide. Formats acceptés: JPG, JPEG, PNG, GIF. Taille max: 5MB");
        }

        // Supprimer l'ancienne image si elle existe
        if (product.getImageFilename() != null) {
            try {
                fileStorageService.deleteFile(product.getImageFilename(), productsDirectory);
            } catch (IOException e) {
                log.warn("Impossible de supprimer l'ancienne image: {}", e.getMessage());
            }
        }

        // Stocker la nouvelle image
        String filename = fileStorageService.generateUniqueFilename(imageFile.getOriginalFilename());
        fileStorageService.storeImage(imageFile, productsDirectory);

        // Mettre à jour le produit
        product.setImageFilename(filename);
        product.setImageUrl(null); // Désactiver l'URL externe si on utilise le stockage local

        productRepository.save(product);

        return getProductImageUrl(productId);
    }

    @Override
    @Transactional
    public void deleteProductImage(UUID productId) throws IOException {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

        if (product.getImageFilename() == null) {
            throw new BadRequestException("Ce produit n'a pas d'image");
        }

        // Supprimer le fichier
        fileStorageService.deleteFile(product.getImageFilename(), productsDirectory);

        // Mettre à jour le produit
        product.setImageFilename(null);
        product.setImageUrl(null);

        productRepository.save(product);
    }

    @Override
    public String getProductImageUrl(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            return product.getImageUrl(); // URL externe
        }

        if (product.getImageFilename() != null && !product.getImageFilename().isEmpty()) {
            return fileStorageService.getFileUrl(product.getImageFilename(), productsDirectory);
        }

        return null; // Pas d'image
    }

    @Override
    @Transactional
    public String updateProductImage(UUID productId, MultipartFile newImageFile) throws IOException {
        // C'est essentiellement la même chose que uploadProductImage
        return uploadProductImage(productId, newImageFile);
    }

    @Override
    public boolean validateProductImage(MultipartFile imageFile) {
        if (imageFile.isEmpty() || imageFile.getSize() > maxImageSize) {
            return false;
        }

        String contentType = imageFile.getContentType();
        if (contentType == null) {
            return false;
        }

        // Vérifier les types MIME autorisés
        return contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp");
    }
}