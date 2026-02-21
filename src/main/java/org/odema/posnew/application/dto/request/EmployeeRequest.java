package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.odema.posnew.domain.enums_old.UserRole;

import java.util.UUID;

public record EmployeeRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,50}$",
                message = "Le nom d'utilisateur doit contenir 3-50 caractères alphanumériques ou underscores")
        String username,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$",
                message = "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule, un chiffre et un caractère spécial")
        String password,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email doit être valide")
        String email,

        @Pattern(regexp = "^\\+?[0-9\\s\\-()]{10,}$",
                message = "Le numéro de téléphone doit être valide")
        String phone,

        String address,

        @NotNull(message = "Le rôle est obligatoire")
        UserRole role,

        @NotNull(message = "Le store est obligatoire")
        UUID storeId
) {
}
