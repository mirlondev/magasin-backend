package org.odema.posnew.domain.service;

import org.odema.posnew.application.dto.CustomerResponse;
import org.odema.posnew.application.dto.request.CustomerRequest;
import org.odema.posnew.application.dto.request.DiscountResult;
import org.odema.posnew.application.dto.response.LoyaltySummaryResponse;
import org.odema.posnew.domain.model.enums.LoyaltyTier;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse getCustomerById(UUID customerId);

    CustomerResponse getCustomerByEmail(String email);

    CustomerResponse getCustomerByPhone(String phone);

    CustomerResponse updateCustomer(UUID customerId, CustomerRequest request);

    void deactivateCustomer(UUID customerId);

    void activateCustomer(UUID customerId);

    List<CustomerResponse> getAllCustomers();

    List<CustomerResponse> searchCustomers(String keyword);

    List<CustomerResponse> getTopCustomers(int limit);

    @Transactional
    DiscountResult usePointsForDiscount(UUID customerId, int points, UUID orderId);

    int calculatePointsEarned(BigDecimal amount, LoyaltyTier tier);

    @Transactional
    void adjustPointsManually(UUID customerId, int delta, String reason, UUID adminId);

    CustomerResponse addLoyaltyPoints(UUID customerId, Integer points);

    CustomerResponse removeLoyaltyPoints(UUID customerId, Integer points);

    // ✅ getCustomerLoyalty — déléguer à LoyaltyService
    @Transactional(readOnly = true)
    LoyaltySummaryResponse getCustomerLoyalty(UUID customerId);
}
