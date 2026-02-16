package org.odema.posnew.design.event;

import lombok.Getter;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.Payment;
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
