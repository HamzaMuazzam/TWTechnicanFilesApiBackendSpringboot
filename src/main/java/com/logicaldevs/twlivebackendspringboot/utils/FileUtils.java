package com.logicaldevs.twlivebackendspringboot.utils;

import com.logicaldevs.twlivebackendspringboot.exceptions.BadRequestException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class FileUtils {
    private static final String UPLOAD_DIR = "src/main/resources/static/files/";
    private static final String PUBLIC_FILES_DIR = "files";

    public static String savePicture(MultipartFile file, boolean isRenamedRequired) {
        if (file == null) return null;
        String fileName = null;
        if (isRenamedRequired) {
            fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        }else{
            fileName = file.getOriginalFilename();
        }

        fileName = validateFileName(fileName);

        // Create the directory if it doesn't exist
        Path uploadPath = Paths.get(System.getProperty("user.dir"), UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                throw new BadRequestException("FAILED_TO_CREATE_DIRECTORY");
            }
        }

        // Define the full file path where the file will be saved
        Path filePath = uploadPath.resolve(fileName);

        try {
            // Save the file bytes to the specified path
            Files.write(filePath, file.getBytes());
        } catch (IOException e) {
            throw new BadRequestException("FAILED_TO_STORE_FILE");
        }

        // Return the file name or path that can be saved in the database
        return getPublicFilePath(fileName);
    }

    public static String getPublicFilePath(String fileName) {
        return PUBLIC_FILES_DIR + "/" + validateFileName(fileName);
    }

    public static boolean fileExists(String fileName) {
        Path uploadPath = Paths.get(System.getProperty("user.dir"), UPLOAD_DIR);
        Path filePath = uploadPath.resolve(validateFileName(fileName));
        return Files.exists(filePath);
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
            throw new BadRequestException("INVALID_FILE_NAME");
        }

        return normalizedFileName;
    }
}
