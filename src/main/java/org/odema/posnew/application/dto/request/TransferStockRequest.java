package org.odema.posnew.application.dto.request;

import java.util.UUID;

public record TransferStockRequest(
        @NotNull UUID productId,
        @NotNull UUID fromStoreId,
        @NotNull UUID toStoreId,
        @NotNull @Positive Integer quantity,
        String notes
) {
}
