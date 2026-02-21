package org.odema.posnew.design.event;

import org.odema.posnew.domain.model.Order;

public class OrderCreatedEvent extends OrderEvent {
    public OrderCreatedEvent(Object source, Order order) {
        super(source, order);
    }
}
