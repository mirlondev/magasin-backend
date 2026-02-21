package org.odema.posnew.application.dto;

import java.util.UUID;

public record InventoryTransferRequest(
        UUID productId,
        UUID fromStoreId,
        UUID toStoreId,
        Integer quantity,
        String notes
) {
}
