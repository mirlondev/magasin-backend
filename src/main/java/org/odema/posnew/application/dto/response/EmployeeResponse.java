package org.odema.posnew.application.dto;

import org.odema.posnew.domain.model.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmployeeResponse(
        UUID employeeId,
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
