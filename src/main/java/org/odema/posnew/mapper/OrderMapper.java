package org.odema.posnew.mapper;

import org.odema.posnew.dto.response.OrderItemResponse;
import org.odema.posnew.dto.response.OrderResponse;
import org.odema.posnew.dto.response.PaymentResponse;
import org.odema.posnew.entity.Order;
import org.odema.posnew.entity.OrderItem;
import org.odema.posnew.entity.Payment;
import org.odema.posnew.entity.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) return null;

        // Map payments
        List<PaymentResponse> paymentResponses = order.getPayments().stream()
                .map(this::toPaymentResponse)
                .collect(Collectors.toList());

        return new OrderResponse(
                order.getOrderId(),
                order.getOrderNumber(),

                order.getCustomer() != null ? order.getCustomer().getCustomerId() : null,
                order.getCustomer() != null ? order.getCustomer().getFullName() : null,
                order.getCustomer() != null ? order.getCustomer().getEmail() : null,
                order.getCustomer() != null ? order.getCustomer().getPhone() : null,

                order.getCashier() != null ? order.getCashier().getUserId() : null,
                order.getCashier() != null ? order.getCashier().getUsername() : null,

                order.getStore() != null ? UUID.fromString(String.valueOf(order.getStore().getStoreId())) : null,
                order.getStore() != null ? order.getStore().getName() : null,

                order.getItems().stream()
                        .map(this::toItemResponse)
                        .collect(Collectors.toList()),

                // Amounts from items
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),

                // Payment info
                order.getAmountPaid(),        // Deprecated
                order.getTotalPaid(),         // NEW
                order.getTotalCredit(),       // NEW
                order.getRemainingAmount(),   // NEW
                order.getChangeAmount(),
                paymentResponses,             // NEW

                // Status
                order.getStatus(),
                order.getPaymentMethod(),     // Deprecated
                order.getPaymentStatus(),

                // Options
                order.getIsTaxable(),
                order.getTaxRate(),
                order.getOrderType(),
                order.getNotes(),

                // Dates
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getCompletedAt(),
                order.getCancelledAt(),

                // Counters
                order.getItems() != null ? order.getItems().size() : 0,
                order.getStatus() == OrderStatus.COMPLETED && !order.isFullyRefunded()
        );
    }

    public OrderItemResponse toItemResponse(OrderItem item) {
        if (item == null) return null;

        return new OrderItemResponse(
                item.getOrderItemId(),
                item.getProduct() != null ? item.getProduct().getProductId() : null,
                item.getProduct() != null ? item.getProduct().getName() : null,
                item.getProduct() != null ? item.getProduct().getSku() : null,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                item.getDiscountPercentage(),
                item.getDiscountAmount(),
                item.getFinalPrice(),
                item.getNotes()
        );
    }

    public PaymentResponse toPaymentResponse(Payment payment) {
        return getPaymentResponse(payment);
    }

    static PaymentResponse getPaymentResponse(Payment payment) {
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
}