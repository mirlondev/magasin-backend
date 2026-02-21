package org.odema.posnew.application.dto.request;

import java.time.LocalDate;

public record UpdateCustomerRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        String city,
        String postalCode,
        String country,
        LocalDate dateOfBirth,
        Boolean isActive
) {
}
