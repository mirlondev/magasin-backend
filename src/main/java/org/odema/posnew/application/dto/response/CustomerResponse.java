package org.odema.posnew.application.dto;

import java.math.BigDecimal;
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
        String loyaltyTier,
        BigDecimal totalPurchases,
        LocalDateTime lastPurchaseDate,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isActive,
        Integer orderCount
) {
}
