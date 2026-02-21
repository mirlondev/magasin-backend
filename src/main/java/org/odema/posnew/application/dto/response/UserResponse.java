package org.odema.posnew.application.dto.response;

import org.odema.posnew.domain.model.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID userId,
        String username,
        String email,
        String phone,
        String address,
        Boolean active,
        UserRole userRole,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLogin
) {
}
