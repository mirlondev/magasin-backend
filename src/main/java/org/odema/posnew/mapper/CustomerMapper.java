package org.odema.posnew.mapper;

import org.odema.posnew.dto.request.CustomerRequest;
import org.odema.posnew.dto.response.CustomerResponse;
import org.odema.posnew.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequest request) {
        if (request == null) return null;

        return Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .city(request.city())
                .postalCode(request.postalCode())
                .country(request.country())
                .isActive(true)
                .build();
    }

    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) return null;

        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getAddress(),
                customer.getCity(),
                customer.getPostalCode(),
                customer.getCountry(),
                customer.getLoyaltyPoints(),
                customer.getTotalPurchases(),
                customer.getLastPurchaseDate(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getIsActive(),
                customer.getOrders() != null ? customer.getOrders().size() : 0
        );
    }
}
