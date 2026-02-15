package org.odema.posnew.design.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import org.odema.posnew.entity.Order;
@Getter
public abstract class OrderEvent extends ApplicationEvent {
    private final Order order;

    public OrderEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }
}

