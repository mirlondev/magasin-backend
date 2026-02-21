package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.odema.posnew.domain.model.enums.StoreType;

import java.math.BigDecimal;

public record CreateStoreRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 500) String address,
        @Size(max = 100) String city,
        @Size(max = 20) String postalCode,
        @Size(max = 100) String country,
        @NotNull StoreType storeType,
        @Pattern(regexp = "^\\+?[0-9\\s\\-\\(\\)]{8,20}$") String phone,
        @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$") String email,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
