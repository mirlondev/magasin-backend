package org.odema.posnew.dto.response;


import java.time.LocalDateTime;
import java.util.UUID;

public record CashRegisterResponse(
        UUID cashRegisterId,
        String registerNumber,
        String name,
        UUID storeId,
        String storeName,
        Boolean isActive,
        String location,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}