package org.odema.posnew.application.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CategoryRequest(
        @NotBlank(message = "Le nom de la catégorie est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        String name,

        String description,

        String imageUrl,

        UUID parentCategoryId
) {}