package org.odema.posnew.application.dto.request;

import org.odema.posnew.domain.model.enums.UserRole;

public record UserRequest(
        String username,
        String password,
        String email,
        String phone,
        String address,
        UserRole userRole
) {
}
