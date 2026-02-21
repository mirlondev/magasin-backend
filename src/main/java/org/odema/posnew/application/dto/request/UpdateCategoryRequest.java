package org.odema.posnew.application.dto.request;

import java.util.UUID;

public record UpdateCategoryRequest(
        String name,
        String description,
        String imageUrl,
        UUID parentCategoryId
) {

}
