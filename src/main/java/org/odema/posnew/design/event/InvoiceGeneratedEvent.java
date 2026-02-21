package org.odema.posnew.design.event;

import lombok.Getter;
import org.odema.posnew.domain.model.Invoice;
import org.odema.posnew.domain.model.Order;
import org.springframework.context.ApplicationEvent;

@Getter
public class InvoiceGeneratedEvent extends ApplicationEvent {
    private final Invoice invoice;
    private final Order order;

    public InvoiceGeneratedEvent(Object source, Invoice invoice, Order order) {
        super(source);
        this.invoice = invoice;
        this.order = order;
    }
}
