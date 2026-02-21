package org.odema.posnew.application.dto.request;


import org.odema.posnew.domain.model.enums.UserRole;

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
