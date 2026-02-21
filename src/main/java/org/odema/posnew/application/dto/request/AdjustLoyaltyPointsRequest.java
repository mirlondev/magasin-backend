package org.odema.posnew.application.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record AdjustLoyaltyPointsRequest(
        @NotNull UUID customerId,
        @NotNull Integer pointsDelta,
        @NotBlank String reason,
        UUID orderId
) {
}
