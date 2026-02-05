package org.odema.posnew.dto;

import org.odema.posnew.entity.enums.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for {@link org.odema.posnew.entity.User}
 */
public record UserDto(UUID userId, String username, String password, String email, String phone, String address,
                      Boolean active,
                      UserRole userRole, LocalDateTime createdAt, LocalDateTime updatedAt,
                      LocalDateTime lastLogin)  {
}