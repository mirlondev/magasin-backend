package org.odema.posnew.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.regex.Pattern;

public record CreateCustomerRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Pattern(regexp = "^\\+?[0-9\\s\\-\\(\\)]{8,20}$") String phone,
        String address,
        @Size(max = 100) String city,
        @Size(max = 20) String postalCode,
        @Size(max = 100) String country,
        LocalDate dateOfBirth
) {
}
