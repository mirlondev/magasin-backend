package org.odema.posnew.application.mapper;

import org.odema.posnew.application.dto.CustomerResponse;
import org.odema.posnew.application.dto.request.CustomerRequest;
import org.odema.posnew.domain.model.Customer;
import org.odema.posnew.domain.model.enums.LoyaltyTier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
                .dateOfBirth(request.dateOfBirth())
                .loyaltyPoints(0)
                .loyaltyTier(LoyaltyTier.BRONZE)
                .purchaseCount(0)
                .totalPurchases(BigDecimal.ZERO)
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
                customer.getLoyaltyTier() != null ? customer.getLoyaltyTier().name() : null,
                customer.getTotalPurchases(),
                customer.getLastPurchaseDate(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getIsActive(),
                customer.getOrders() != null ? customer.getOrders().size() : 0
        );
    }

    public List<CustomerResponse> toResponseList(List<Customer> customers) {
        if (customers == null) return List.of();
        return customers.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}