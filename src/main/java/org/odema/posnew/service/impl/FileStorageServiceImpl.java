package org.odema.posnew.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.odema.posnew.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.file.storage-path:uploads}")
    private String storagePath;

    @Value("${app.file.max-size:10485760}")
    private long maxFileSize;

    @Value("${app.file.allowed-image-extensions:jpg,jpeg,png,gif,webp}")
    private String[] allowedImageExtensions;

    @Value("${app.file.allowed-file-extensions:pdf,doc,docx,xls,xlsx,txt,csv}")
    private String[] allowedFileExtensions;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Override
    public String storeFile(MultipartFile file, String directory) throws IOException {
        if (!isValidFile(file)) {
            throw new IOException("Type de fichier non autorisé ou taille excessive");
        }

        String filename = generateUniqueFilename(file.getOriginalFilename());
        Path targetLocation = getFilePath(filename, directory);

        Files.createDirectories(targetLocation.getParent());
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        log.info("Fichier stocké: {}", targetLocation);
        return filename;
    }

    @Override
    public String storeImage(MultipartFile imageFile, String directory) throws IOException {
        if (!isValidImage(imageFile)) {
            throw new IOException("Format d'image non supporté ou taille excessive");
        }

        // Vous pouvez ajouter ici du traitement d'image (redimensionnement, compression, etc.)
        return storeFile(imageFile, directory);
    }

    @Override
    public Resource loadFile(String filename, String directory) throws MalformedURLException {
        Path filePath = getFilePath(filename, directory);
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Impossible de lire le fichier: " + filename);
        }
    }

    @Override
    public void deleteFile(String filename, String directory) throws IOException {
        Path filePath = getFilePath(filename, directory);
        Files.deleteIfExists(filePath);
    }

    @Override
    public List<String> listFiles(String directory) throws IOException {
        Path dirPath = Paths.get(storagePath).resolve(directory);
        if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
            return Files.list(dirPath)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public String getFileUrl(String filename, String directory) {
        return baseUrl + "/uploads/" + directory + "/" + filename;
    }

    @Override
    public String generateUniqueFilename(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String baseName = UUID.randomUUID().toString();
        return extension != null ? baseName + "." + extension : baseName;
    }

    @Override
    public boolean isValidImage(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > maxFileSize) {
            return false;
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        return extension != null && Arrays.asList(allowedImageExtensions)
                .contains(extension.toLowerCase());
    }

    @Override
    public boolean isValidFile(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > maxFileSize) {
            return false;
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null) return false;

        String lowerExtension = extension.toLowerCase();
        return Arrays.asList(allowedImageExtensions).contains(lowerExtension) ||
                Arrays.asList(allowedFileExtensions).contains(lowerExtension);
    }

    @Override
    public long getFileSize(MultipartFile file) {
        return file.getSize();
    }
    @Override
    public String storeFileFromBytes(byte[] fileBytes, String filename, String directory) throws IOException {
        Path targetLocation = getFilePath(filename, directory);
        Files.createDirectories(targetLocation.getParent());
        Files.write(targetLocation, fileBytes);
        return filename;
    }
    private Path getFilePath(String filename, String directory) {
        return Paths.get(storagePath).resolve(directory).resolve(filename).normalize();
    }
}