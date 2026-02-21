package org.odema.posnew.design.event;

import org.springframework.context.ApplicationEvent;

public abstract class StockEvent extends ApplicationEvent {
    public StockEvent(Object source) {
        super(source);
    }
}