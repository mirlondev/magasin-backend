package org.odema.posnew.application.dto.response;

import org.odema.posnew.domain.enums_old.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeeResponse(
        UUID userId,
        String username,
        String email,
        String phone,
        String address,
        Boolean active,
        UserRole role,
        UUID storeId,
        String storeName,
        String storeType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLogin
) {
}
