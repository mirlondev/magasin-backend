package org.odema.posnew.application.dto.request;

import java.util.UUID;

public record CashRegisterRequest(
        String registerNumber,
        String name,
        UUID storeId,
        String location,
        String serialNumber,
        String model) {
}
