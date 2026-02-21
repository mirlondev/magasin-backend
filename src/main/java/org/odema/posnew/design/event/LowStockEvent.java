package org.odema.posnew.design.event;

import lombok.Getter;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class LowStockEvent extends StockEvent {
    private final UUID productId;
    private final String productName;
    private final UUID storeId;
    private final String storeName;
    private final int currentQuantity;
    private final int reorderPoint;

    public LowStockEvent(Object source, UUID productId, String productName,
                         UUID storeId, String storeName, int currentQuantity, int reorderPoint) {
        super(source);
        this.productId = productId;
        this.productName = productName;
        this.storeId = storeId;
        this.storeName = storeName;
        this.currentQuantity = currentQuantity;
        this.reorderPoint = reorderPoint;
    }
}