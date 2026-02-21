package org.odema.posnew.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID customerId,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        String address,
        String city,
        String postalCode,
        String country,
        Integer loyaltyPoints,
        Double totalPurchases,
        LocalDateTime lastPurchaseDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isActive,
        Integer totalOrders
) {
}
