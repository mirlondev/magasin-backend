package org.odema.posnew.dto;

import org.odema.posnew.entity.enums.StoreStatus;
import org.odema.posnew.entity.enums.StoreType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link org.odema.posnew.entity.Store}
 */
public record StoreDto(
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
        java.math.BigDecimal latitude,
        java.math.BigDecimal longitude,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isActive,          // 👈 ICI
        UUID storeAdminId          // 👈 ET ICI
) {}
