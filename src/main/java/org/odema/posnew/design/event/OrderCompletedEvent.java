package org.odema.posnew.design.event;
import org.odema.posnew.entity.Order;
public class OrderCompletedEvent extends OrderEvent {
    public OrderCompletedEvent(Object source, Order order) {
        super(source, order);
    }
}
