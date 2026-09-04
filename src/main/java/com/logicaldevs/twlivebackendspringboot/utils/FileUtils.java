package com.logicaldevs.twlivebackendspringboot.utils;

import com.logicaldevs.twlivebackendspringboot.exceptions.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
public class FileUtils {
    private static final String UPLOAD_DIR = "src/main/resources/static/files/";
    private static final String PUBLIC_FILES_DIR = "files";

    /** Absolute directory uploads are written to (user.dir + UPLOAD_DIR). */
    public static Path getUploadDirectory() {
        return Paths.get(System.getProperty("user.dir"), UPLOAD_DIR).toAbsolutePath().normalize();
    }

    public static String savePicture(MultipartFile file, boolean isRenamedRequired) {
        if (file == null) {
            log.warn("[FileUtils] savePicture called with null file");
            return null;
        }
        String fileName = null;
        if (isRenamedRequired) {
            fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        }else{
            fileName = file.getOriginalFilename();
        }

        fileName = validateFileName(fileName);

        // Create the directory if it doesn't exist
        Path uploadPath = getUploadDirectory();
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
                log.info("[FileUtils] created upload directory {}", uploadPath);
            } catch (IOException e) {
                log.error("[FileUtils] cannot create upload directory {}: {}", uploadPath, e.getMessage());
                throw new BadRequestException("FAILED_TO_CREATE_DIRECTORY");
            }
        }

        // Define the full file path where the file will be saved
        Path filePath = uploadPath.resolve(fileName);
        boolean overwriting = Files.exists(filePath);

        try {
            // Save the file bytes to the specified path
            Files.write(filePath, file.getBytes());
            log.info("[FileUtils] wrote {} bytes to {}{}", file.getSize(), filePath, overwriting ? " (overwrote existing file)" : "");
        } catch (IOException e) {
            log.error("[FileUtils] failed to write {}: {}", filePath, e.getMessage());
            throw new BadRequestException("FAILED_TO_STORE_FILE");
        }

        // Return the file name or path that can be saved in the database
        return getPublicFilePath(fileName);
    }

    public static String getPublicFilePath(String fileName) {
        return PUBLIC_FILES_DIR + "/" + validateFileName(fileName);
    }

    public static boolean fileExists(String fileName) {
        Path filePath = getUploadDirectory().resolve(validateFileName(fileName));
        boolean exists = Files.exists(filePath);
        log.debug("[FileUtils] exists? {} -> {}", filePath, exists);
        return exists;
    }

    private static String validateFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new BadRequestException("FILE_NAME_REQUIRED");
        }

        String normalizedFileName = StringUtils.cleanPath(fileName).trim();
        if (!StringUtils.hasText(normalizedFileName)) {
            throw new BadRequestException("FILE_NAME_REQUIRED");
        }

        if (normalizedFileName.contains("..") || normalizedFileName.contains("/") || normalizedFileName.contains("\\")) {
            log.warn("[FileUtils] rejected invalid file name '{}'", fileName);
            throw new BadRequestException("INVALID_FILE_NAME");
        }

        return normalizedFileName;
    }
}
