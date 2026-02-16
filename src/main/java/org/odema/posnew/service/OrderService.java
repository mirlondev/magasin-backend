package org.odema.posnew.service;

import org.odema.posnew.dto.request.PaymentRequest;
import org.odema.posnew.dto.response.OrderResponse;
import org.odema.posnew.dto.request.OrderRequest;
import org.odema.posnew.exception.UnauthorizedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderService {

    // Order creation (no payment)
    @Transactional
    OrderResponse createOrder(OrderRequest request, UUID cashierId) throws UnauthorizedException;

    // Order creation with payment (convenience method)
    @Transactional
    OrderResponse createOrderWithPayment(OrderRequest orderRequest,
                                         PaymentRequest paymentRequest,
                                         UUID cashierId) throws UnauthorizedException;

    // Payment handling
    @Transactional
    OrderResponse addPaymentToOrder(UUID orderId, PaymentRequest paymentRequest, UUID cashierId)
            throws UnauthorizedException;

    // Order lifecycle
    @Transactional
    OrderResponse markAsCompleted(UUID orderId);

    @Transactional
    void cancelOrder(UUID orderId);

    @Transactional
    OrderResponse updateOrder(UUID orderId, OrderRequest request);

    // Queries
    OrderResponse getOrderById(UUID orderId);
    OrderResponse getOrderByNumber(String orderNumber);
    List<OrderResponse> getAllOrders();
    Page<OrderResponse> getOrders(UUID userId, Pageable pageable);
    List<OrderResponse> getOrdersByStore(UUID storeId);
    List<OrderResponse> getOrdersByCustomer(UUID customerId);
    List<OrderResponse> getOrdersByCashier(UUID cashierId);
    List<OrderResponse> getOrdersByStatus(String status);
    List<OrderResponse> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    List<OrderResponse> getRecentOrders(int limit);
    List<OrderResponse> findCashierOrdersByShift(UUID cashierId, UUID shiftId);

    // Statistics
    BigDecimal getTotalSalesByStore(UUID storeId, LocalDateTime startDate, LocalDateTime endDate);
    Integer getOrderCountByStore(UUID storeId, LocalDateTime startDate, LocalDateTime endDate);
}