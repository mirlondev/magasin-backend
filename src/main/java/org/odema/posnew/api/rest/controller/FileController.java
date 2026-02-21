package org.odema.posnew.api.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.odema.posnew.application.dto.response.ApiResponse;
import org.odema.posnew.application.dto.response.FileUploadResponse;
import org.odema.posnew.application.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "API de gestion des fichiers")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload/{directory}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Uploader un fichier")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @PathVariable String directory,
            @RequestParam("file") MultipartFile file) {

        try {
            String filename = fileStorageService.storeFile(file, directory);
            String fileUrl = fileStorageService.getFileUrl(filename, directory);

            FileUploadResponse response = new FileUploadResponse(
                    filename,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    fileUrl
            );

            return ResponseEntity.ok(ApiResponse.success("Fichier uploadé avec succès", response));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de l'upload du fichier: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-images/{directory}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Uploader une image")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadImage(
            @PathVariable String directory,
            @RequestParam("image") MultipartFile image) {

        if (!fileStorageService.isValidImage(image)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Format d'image non supporté ou taille excessive"));
        }

        try {
            String filename = fileStorageService.storeImage(image, directory);
            String fileUrl = fileStorageService.getFileUrl(filename, directory);

            FileUploadResponse response = new FileUploadResponse(
                    filename,
                    image.getOriginalFilename(),
                    image.getSize(),
                    image.getContentType(),
                    fileUrl
            );

            return ResponseEntity.ok(ApiResponse.success("Image uploadée avec succès", response));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de l'upload de l'image: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{directory}/{filename:.+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Télécharger un fichier")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String directory,
            @PathVariable String filename) {

        try {
            Resource resource = fileStorageService.loadFile(filename, directory);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/view/{directory}/{filename:.+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER', 'EMPLOYEE')")
    @Operation(summary = "Voir un fichier (image, PDF)")
    public ResponseEntity<Resource> viewFile(
            @PathVariable String directory,
            @PathVariable String filename) {

        try {
            Resource resource = fileStorageService.loadFile(filename, directory);
            String contentType = determineContentType(filename);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/list/{directory}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Lister les fichiers d'un répertoire")
    public ResponseEntity<ApiResponse<List<String>>> listFiles(
            @PathVariable String directory) {

        try {
            List<String> files = fileStorageService.listFiles(directory);
            return ResponseEntity.ok(ApiResponse.success(files));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la lecture du répertoire"));
        }
    }

    @DeleteMapping("/{directory}/{filename:.+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER')")
    @Operation(summary = "Supprimer un fichier")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable String directory,
            @PathVariable String filename) {

        try {
            fileStorageService.deleteFile(filename, directory);
            return ResponseEntity.ok(ApiResponse.success("Fichier supprimé avec succès", null));
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Erreur lors de la suppression du fichier"));
        }
    }

    @PostMapping("/upload-multiple/{directory}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'SHOP_MANAGER', 'CASHIER')")
    @Operation(summary = "Uploader plusieurs fichiers")
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> uploadMultipleFiles(
            @PathVariable String directory,
            @RequestParam("files") MultipartFile[] files) {

        List<FileUploadResponse> responses = Arrays.stream(files)
                .map(file -> {
                    try {
                        String filename = fileStorageService.storeFile(file, directory);
                        String fileUrl = fileStorageService.getFileUrl(filename, directory);

                        return new FileUploadResponse(
                                filename,
                                file.getOriginalFilename(),
                                file.getSize(),
                                file.getContentType(),
                                fileUrl
                        );
                    } catch (IOException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Fichiers uploadés avec succès", responses));
    }

    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }
}