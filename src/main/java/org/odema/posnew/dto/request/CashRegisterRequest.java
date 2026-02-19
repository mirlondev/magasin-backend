package org.odema.posnew.dto.request;

import java.util.UUID;

public record CashRegisterRequest(
        String registerNumber,
        String name,
        UUID storeId,
        String location
) {
}
