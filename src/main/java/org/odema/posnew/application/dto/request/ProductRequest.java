package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank(message = "Le nom du produit est obligatoire")
        @Size(min = 2, max = 200, message = "Le nom doit contenir entre 2 et 200 caractères")
        String name,
//
        String description,

        @NotNull(message = "Le prix est obligatoire")
        @DecimalMin(value = "0.01", message = "Le prix doit être supérieur à 0")
        @DecimalMax(value = "9999999.99", message = "Le prix ne peut pas dépasser 9,999,999.99")
        BigDecimal price,

        @NotNull(message = "La quantité est obligatoire")
        @Min(value = 0, message = "La quantité ne peut pas être négative")
        Integer quantity,

        @NotNull(message = "La catégorie est obligatoire")
        UUID categoryId,

        String imageUrl,

        @Size(max = 50, message = "Le SKU ne peut pas dépasser 50 caractères")
        String sku,

        @Size(max = 50, message = "Le code-barres ne peut pas dépasser 50 caractères")
        String barcode,

        UUID storeId
) {}