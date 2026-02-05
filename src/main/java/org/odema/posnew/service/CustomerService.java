package org.odema.posnew.service;

import org.odema.posnew.dto.request.CustomerRequest;
import org.odema.posnew.dto.response.CustomerResponse;

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

    CustomerResponse addLoyaltyPoints(UUID customerId, Integer points);

    CustomerResponse removeLoyaltyPoints(UUID customerId, Integer points);
}
