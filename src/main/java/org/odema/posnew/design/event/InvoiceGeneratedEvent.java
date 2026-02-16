package org.odema.posnew.design.event;

import org.odema.posnew.entity.Invoice;
import org.odema.posnew.service.impl.InvoiceServiceImpl;
import org.springframework.context.ApplicationEvent;

public class InvoiceGeneratedEvent extends ApplicationEvent {
    public InvoiceGeneratedEvent(Object source,Invoice savedInvoice) {
        super(source);
    }
}
