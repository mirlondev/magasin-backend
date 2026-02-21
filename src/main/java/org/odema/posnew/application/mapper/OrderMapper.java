package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.request.OrderItemRequest;
import org.odema.posnew.application.dto.request.OrderRequest;
import org.odema.posnew.application.dto.response.OrderItemResponse;
import org.odema.posnew.application.dto.response.OrderResponse;
import org.odema.posnew.application.dto.response.PaymentResponse;
import org.odema.posnew.domain.model.Order;
import org.odema.posnew.domain.model.OrderItem;
import org.odema.posnew.domain.model.Payment;
import org.odema.posnew.domain.model.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) return null;

        return new OrderResponse(
                order.getOrderId(),
                order.getOrderNumber(),
                order.getCustomer() != null ? order.getCustomer().getCustomerId() : null,
                order.getCustomer() != null ? order.getCustomer().getFullName() : null,
                order.getCustomer() != null ? order.getCustomer().getEmail() : null,
                order.getCustomer() != null ? order.getCustomer().getPhone() : null,
                order.getCashier() != null ? order.getCashier().getUserId() : null,
                order.getCashier() != null ? order.getCashier().getUsername() : null,
                order.getStore() != null ? order.getStore().getStoreId() : null,
                order.getStore() != null ? order.getStore().getName() : null,
                mapItems(order.getItems()),
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getGlobalDiscount(),
                order.getTotalAmount(),
                order.getTotalPaid(),
                order.getTotalCredit(),
                order.getRemainingAmount(),
                order.getChangeAmount(),
                mapPayments(order.getPayments()),
                order.getStatus(),
                order.getPrimaryPaymentMethod(),
                order.getPaymentStatus(),
                null, // isTaxable - not in entity
                null, // taxRate - not in entity
                order.getOrderType(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getCompletedAt(),
                order.getCancelledAt(),
                order.getItems() != null ? order.getItems().size() : 0,
                order.getStatus() == OrderStatus.COMPLETED
                        && !order.getRefunds().stream().map(
                        refund -> refund.getItems() != null ? refund.getItems().size() : 0
                ).isParallel()
        );
    }

    private List<OrderItemResponse> mapItems(List<OrderItem> items) {
        if (items == null) return List.of();
        return items.stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) return null;

        return new OrderItemResponse(
                item.getOrderItemId(),
                item.getProduct() != null ? item.getProduct().getProductId() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getProduct() != null ? item.getProduct().getSku() : null,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getFinalPrice(),
                item.getDiscountPercentage(),
                item.getDiscountAmount(),
                item.getFinalPrice(),
                item.getNotes()
        );
    }

    private List<PaymentResponse> mapPayments(List<Payment> payments) {
        if (payments == null) return List.of();
        return payments.stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        if (payment == null) return null;

        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrder() != null ? payment.getOrder().getOrderId() : null,
                payment.getOrder() != null ? payment.getOrder().getOrderNumber() : null,
                payment.getMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getCashier() != null ? payment.getCashier().getUserId() : null,
                payment.getCashier() != null ? payment.getCashier().getUsername() : null,
                payment.getShiftReport() != null ? payment.getShiftReport().getShiftReportId() : null,
                payment.getNotes(),
                payment.getCreatedAt()
        );
    }

    public List<OrderResponse> toResponseList(List<Order> orders) {
        if (orders == null) return List.of();
        return orders.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}