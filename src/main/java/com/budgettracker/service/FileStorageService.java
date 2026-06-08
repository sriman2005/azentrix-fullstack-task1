package com.budgettracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalFileName.substring(dotIndex);
            }

            // Validate extension
            String ext = extension.toLowerCase();
            if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png")
                    && !ext.equals(".gif") && !ext.equals(".webp")) {
                throw new RuntimeException("Only image files are allowed (jpg, jpeg, png, gif, webp)");
            }

            // Validate file size (10MB max)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("File size must not exceed 10MB");
            }

            String uniqueFileName = UUID.randomUUID().toString() + extension;
            Path targetLocation = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/api/files/" + uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    public Path loadFile(String fileName) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        return uploadPath.resolve(fileName).normalize();
    }

    public void deleteFile(String fileUrl) {
        try {
            if (fileUrl != null && fileUrl.startsWith("/api/files/")) {
                String fileName = fileUrl.substring("/api/files/".length());
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
                Path filePath = uploadPath.resolve(fileName).normalize();
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            // Log but don't fail
        }
    }
}
