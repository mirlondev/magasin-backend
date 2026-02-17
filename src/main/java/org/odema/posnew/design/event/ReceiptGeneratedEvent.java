package org.odema.posnew.design.event;

// event/ReceiptGeneratedEvent.java

import lombok.Getter;
import org.odema.posnew.entity.Invoice;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.Receipt;
import org.springframework.context.ApplicationEvent;

@Getter
public class ReceiptGeneratedEvent extends ApplicationEvent {
    private final Receipt receipt;
    private final Order order;

    public ReceiptGeneratedEvent(Object source, Receipt receipt, Order order) {
        super(source);
        this.receipt = receipt;
        this.order = order;
    }
}

