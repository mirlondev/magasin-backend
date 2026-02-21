package org.odema.posnew.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ActivityLogResponse(
        UUID activityId,
        String activityType, // ORDER_CREATED, USER_LOGIN, STOCK_UPDATE, etc.
        String description,
        UUID userId,
        String userName,
        UUID entityId,
        String entityType,
        LocalDateTime timestamp,
        String ipAddress,
        String userAgent
) {
}
