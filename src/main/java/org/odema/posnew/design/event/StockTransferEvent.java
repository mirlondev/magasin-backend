package org.odema.posnew.design.event;

import lombok.Getter;
import java.util.UUID;

@Getter
public class StockTransferEvent extends StockEvent {
    private final UUID productId;
    private final String productName;
    private final UUID fromStoreId;
    private final String fromStoreName;
    private final UUID toStoreId;
    private final String toStoreName;
    private final int quantity;

    public StockTransferEvent(Object source, UUID productId, String productName,
                              UUID fromStoreId, String fromStoreName,
                              UUID toStoreId, String toStoreName, int quantity) {
        super(source);
        this.productId = productId;
        this.productName = productName;
        this.fromStoreId = fromStoreId;
        this.fromStoreName = fromStoreName;
        this.toStoreId = toStoreId;
        this.toStoreName = toStoreName;
        this.quantity = quantity;
    }
}