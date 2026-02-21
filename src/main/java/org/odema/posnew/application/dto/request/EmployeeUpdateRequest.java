package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.odema.posnew.domain.enums_old.UserRole;

import java.util.UUID;

public record EmployeeUpdateRequest(
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$",
                message = "Le nom d'utilisateur doit contenir 3-50 caractères alphanumériques ou underscores")
        String username,

        @Email(message = "L'email doit être valide")
        String email,

        @Pattern(regexp = "^\\+?[0-9\\s\\-()]{10,}$",
                message = "Le numéro de téléphone doit être valide")
        String phone,

        String address,

        UserRole role,

        UUID storeId,

        Boolean active
) {
}
