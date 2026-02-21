package org.odema.posnew.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AdjustLoyaltyPointsRequest(
        @NotNull UUID customerId,
        @NotNull Integer pointsDelta,
        @NotBlank String reason,
        UUID orderId
) {
}
