package org.odema.posnew.dto.response;


public record FileUploadResponse(
        String filename,
        String originalFilename,
        long size,
        String contentType,
        String fileUrl
) {}