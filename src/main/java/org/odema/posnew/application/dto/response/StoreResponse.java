package org.odema.posnew.application.dto.response;

import org.odema.posnew.domain.model.enums.StoreStatus;
import org.odema.posnew.domain.model.enums.StoreType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StoreResponse(
        UUID storeId,
        String name,
        String address,
        String city,
        String postalCode,
        String country,
        StoreType storeType,
        StoreStatus status,
        String phone,
        String email,
        String openingHours,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isActive,
        UUID storeAdminId
) {
}
