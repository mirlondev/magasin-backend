package org.odema.posnew.application.dto.request;

import org.odema.posnew.domain.model.enums.UserRole;

import java.util.UUID;

public record EmployeeUpdateRequest(
        String username,
        String email,
        String phone,
        String address,
        UserRole role,
        Boolean active,
        UUID storeId
) {
}
