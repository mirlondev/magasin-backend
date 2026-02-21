package org.odema.posnew.design.event;

import lombok.Getter;
import org.odema.posnew.domain.model.Order;
import org.springframework.context.ApplicationEvent;

@Getter
public abstract class OrderEvent extends ApplicationEvent {
    private final Order order;

    public OrderEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }
}

