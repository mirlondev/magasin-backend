package org.odema.posnew.service;

import org.odema.posnew.dto.response.OrderResponse;
import org.odema.posnew.dto.request.OrderRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request, UUID cashierId);

    OrderResponse getOrderById(UUID orderId);

    OrderResponse getOrderByNumber(String orderNumber);

    OrderResponse updateOrder(UUID orderId, OrderRequest request);

    void cancelOrder(UUID orderId);

    List<OrderResponse> getAllOrders();

    List<OrderResponse> getOrdersByStore(UUID storeId);

    List<OrderResponse> getOrdersByCustomer(UUID customerId);

    List<OrderResponse> getOrdersByCashier(UUID cashierId);

    List<OrderResponse> getOrdersByStatus(String status);

    List<OrderResponse> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    OrderResponse processPayment(UUID orderId, BigDecimal amountPaid);

    OrderResponse markAsCompleted(UUID orderId);

    BigDecimal getTotalSalesByStore(UUID storeId, LocalDateTime startDate, LocalDateTime endDate);

    Integer getOrderCountByStore(UUID storeId, LocalDateTime startDate, LocalDateTime endDate);

    List<OrderResponse> getRecentOrders(int limit);
    public Page<OrderResponse> getOrders(UUID userId, Pageable pageable) ;
}
