package org.odema.posnew.application.dto;

import java.util.UUID;

public record CategoryRequest(
        String name,
        String description,
        String imageUrl,
        UUID parentCategoryId
) {
}
