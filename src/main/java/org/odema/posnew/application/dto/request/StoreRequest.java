package org.odema.posnew.application.dto.request;

import org.odema.posnew.domain.model.enums.StoreStatus;
import org.odema.posnew.domain.model.enums.StoreType;

import java.math.BigDecimal;

public record StoreRequest(
        String name,
        String address,
        String city,
        String postalCode,
        String country,
        StoreType storeType,
        StoreStatus status,
        String phone,
        String email,
        String openingHours,
        BigDecimal latitude,
        BigDecimal longitude,
        Boolean isActive
) {
}
