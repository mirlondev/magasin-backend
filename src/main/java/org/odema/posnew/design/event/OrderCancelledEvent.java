package org.odema.posnew.design.event;
import org.odema.posnew.entity.Order;
public class OrderCancelledEvent extends OrderEvent {
    public OrderCancelledEvent(Object source, Order order) {
        super(source, order);
    }
}
