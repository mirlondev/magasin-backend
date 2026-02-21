package org.odema.posnew.application.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public interface FileStorageService {
    String storeFile(MultipartFile file, String directory) throws IOException;
    String storeImage(MultipartFile imageFile, String directory) throws IOException;
    Resource loadFile(String filename, String directory) throws MalformedURLException;
    void deleteFile(String filename, String directory) throws IOException;
    List<String> listFiles(String directory) throws IOException;
    String getFileUrl(String filename, String directory);
    String generateUniqueFilename(String originalFilename);
    boolean isValidImage(MultipartFile file);
    boolean isValidFile(MultipartFile file);
    long getFileSize(MultipartFile file);
    String storeFileFromBytes(byte[] fileBytes, String filename, String directory) throws IOException;
    byte[] readFileAsBytes(String filename, String directory) throws IOException;


}