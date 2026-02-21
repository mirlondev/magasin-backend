package org.odema.posnew.application.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface ProductImageService {
    String uploadProductImage(UUID productId, MultipartFile imageFile) throws IOException;

    void deleteProductImage(UUID productId) throws IOException;

    String getProductImageUrl(UUID productId);

    String updateProductImage(UUID productId, MultipartFile newImageFile) throws IOException;

    boolean validateProductImage(MultipartFile imageFile);
}
