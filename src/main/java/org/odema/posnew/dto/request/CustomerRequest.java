package org.odema.posnew.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomerRequest(
        @NotBlank(message = "Le prénom est obligatoire")
        @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]{2,100}$",
                message = "Le prénom doit contenir 2-100 caractères alphabétiques")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire")
        @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]{2,100}$",
                message = "Le nom doit contenir 2-100 caractères alphabétiques")
        String lastName,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email doit être valide")
        String email,

        @NotBlank(message = "Le téléphone est obligatoire")
        @Pattern(regexp = "^\\+?[0-9\\s\\-\\(\\)]{10,20}$",
                message = "Le numéro de téléphone doit être valide")
        String phone,

        String address,

        String city,

        String postalCode,

        String country
) {
}
