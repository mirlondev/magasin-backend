package org.odema.posnew.mapper;

import org.odema.posnew.entity.Order;
import org.odema.posnew.dto.response.OrderItemResponse;
import org.odema.posnew.dto.response.OrderResponse;
import org.odema.posnew.entity.OrderItem;
import org.odema.posnew.entity.enums.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;
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

                order.getStore() != null ? UUID.fromString(String.valueOf(order.getStore().getStoreId())) : null,
                order.getStore() != null ? order.getStore().getName() : null,

                order.getItems().stream()
                        .map(this::toItemResponse)
                        .collect(Collectors.toList()),

                order.getSubtotal(),
                order.getTaxAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getAmountPaid(),
                order.getChangeAmount(),

                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),

                order.getIsTaxable(),
                order.getTaxRate(),

                order.getNotes(),

                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getCompletedAt(),
                order.getCancelledAt(),

                order.getItems() != null ? order.getItems().size() : 0,
                order.getStatus() == OrderStatus.COMPLETED &&
                        !order.isFullyRefunded()
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
}
