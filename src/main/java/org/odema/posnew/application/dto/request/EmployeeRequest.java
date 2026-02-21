package org.odema.posnew.application.dto.request;

import org.odema.posnew.domain.model.enums.UserRole;

import java.util.UUID;

public record EmployeeRequest(
        String username,
        String password,
        String email,
        String phone,
        String address,
        UserRole role,
        UUID storeId
) {
}
