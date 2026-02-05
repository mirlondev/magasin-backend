package org.odema.posnew.dto.request;

import org.odema.posnew.entity.enums.UserRole;

public record RegisterRequest(
    String username,
    String password,
    String email,
    String firstName,
    String lastName,
    String phone,
    String address,
    UserRole role
) {
}
