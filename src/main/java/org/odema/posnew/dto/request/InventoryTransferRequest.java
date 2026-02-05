package org.odema.posnew.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InventoryTransferRequest(
        @NotNull(message = "Le store source est obligatoire")
        UUID fromStoreId,

        @NotNull(message = "Le store destination est obligatoire")
        UUID toStoreId,

        @NotNull(message = "Le produit est obligatoire")
        UUID productId,

        @NotNull(message = "La quantité est obligatoire")
        @Min(value = 1, message = "La quantité doit être au moins 1")
        Integer quantity,

        String notes
) {
}
