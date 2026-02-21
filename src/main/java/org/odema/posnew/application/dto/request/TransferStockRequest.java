package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record TransferStockRequest(
        @NotNull UUID productId,
        @NotNull UUID fromStoreId,
        @NotNull UUID toStoreId,
        @NotNull @Positive Integer quantity,
        String notes
) {
}
