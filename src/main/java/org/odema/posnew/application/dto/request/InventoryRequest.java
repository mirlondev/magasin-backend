package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryRequest(
        @NotNull(message = "Le produit est obligatoire")
        UUID productId,

        @NotNull(message = "Le store est obligatoire")
        UUID storeId,

        @NotNull(message = "La quantité est obligatoire")
        @Min(value = 0, message = "La quantité ne peut pas être négative")
        Integer quantity,

        BigDecimal unitCost,

        BigDecimal sellingPrice,

        @Min(value = 0, message = "Le point de réapprovisionnement ne peut pas être négatif")
        Integer reorderPoint,

        @Min(value = 1, message = "Le stock maximum doit être au moins 1")
        Integer maxStock,

        @Min(value = 0, message = "Le stock minimum ne peut pas être négatif")
        Integer minStock,

        String notes
) {
}
