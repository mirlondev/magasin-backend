package org.odema.posnew.application.dto.response;


public record FileUploadResponse(
        String filename,
        String originalFilename,
        long size,
        String contentType,
        String fileUrl
) {}