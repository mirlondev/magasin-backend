package org.odema.posnew.design.event;

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
