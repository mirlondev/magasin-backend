package org.odema.posnew.design.event;

import lombok.Getter;
import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.Payment;
import org.springframework.context.ApplicationEvent;

@Getter
public class PaymentReceivedEvent extends ApplicationEvent {
    private final Order order;
    private final Payment payment;

    public PaymentReceivedEvent(Object source, Order order, Payment payment) {
        super(source);
        this.order = order;
        this.payment = payment;
    }
}
