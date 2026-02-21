package org.odema.posnew.design.event;

import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class StockAdjustmentEvent extends StockEvent {
    private final UUID inventoryId;
    private final UUID productId;
    private final String productName;
    private final UUID storeId;
    private final String operation;
    private final int quantityChanged;
    private final int newTotalQuantity;
    private final String reason;

    // ✅ RENOMMÉ pour éviter le conflit
    private final LocalDateTime adjustmentTimestamp;

    public StockAdjustmentEvent(Object source, UUID inventoryId, UUID productId, String productName,
                                UUID storeId, String operation, int quantityChanged,
                                int newTotalQuantity, String reason) {
        super(source);
        this.inventoryId = inventoryId;
        this.productId = productId;
        this.productName = productName;
        this.storeId = storeId;
        this.operation = operation;
        this.quantityChanged = quantityChanged;
        this.newTotalQuantity = newTotalQuantity;
        this.reason = reason;
        this.adjustmentTimestamp = LocalDateTime.now();
    }
}