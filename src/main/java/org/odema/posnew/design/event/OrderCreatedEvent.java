package org.odema.posnew.design.event;

public class OrderCreatedEvent extends OrderEvent {
    public OrderCreatedEvent(Object source, Order order) {
        super(source, order);
    }
}
