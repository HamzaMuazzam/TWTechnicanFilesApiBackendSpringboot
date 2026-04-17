package com.logicaldevs.twlivebackendspringboot.modules.fileUploader.service;

import com.logicaldevs.twlivebackendspringboot.exceptions.BadRequestException;
import com.logicaldevs.twlivebackendspringboot.modules.fileUploader.mappers.UploadFileMapper;
import com.logicaldevs.twlivebackendspringboot.responseparser.ResponseWrapper;
import com.logicaldevs.twlivebackendspringboot.utils.FileUtils;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class FileUploaderService {

    private final String contextPath;
    private final String staticPathPattern;

    public FileUploaderService(
            @Value("${server.servlet.context-path:}") String contextPath,
            @Value("${spring.mvc.static-path-pattern:/**}") String staticPathPattern
    ) {
        this.contextPath = contextPath;
        this.staticPathPattern = staticPathPattern;
    }

    public ResponseWrapper<?> saveFile(@Valid UploadFileMapper request) {
        return ResponseWrapper.success(FileUtils.savePicture(request.getFile(),false));
    }

    public ResponseWrapper<?> getPublicFilePath(String fileName) {
        if (!FileUtils.fileExists(fileName)) {
            throw new BadRequestException("FILE_NOT_FOUND");
        }

        return ResponseWrapper.success(buildPublicPath(FileUtils.getPublicFilePath(fileName)));
    }

    private String buildPublicPath(String relativeFilePath) {
        String staticPathPrefix = resolveStaticPathPrefix();
        if (!StringUtils.hasText(staticPathPrefix)) {
            return joinPaths(contextPath, relativeFilePath);
        }

        return joinPaths(contextPath, staticPathPrefix, relativeFilePath);
    }

    private String resolveStaticPathPrefix() {
        if (!StringUtils.hasText(staticPathPattern) || "/**".equals(staticPathPattern.trim())) {
            return "";
        }

        String normalizedPattern = staticPathPattern.trim();
        if (normalizedPattern.endsWith("/**")) {
            return normalizedPattern.substring(0, normalizedPattern.length() - 3);
        }

        if (normalizedPattern.endsWith("/*")) {
            return normalizedPattern.substring(0, normalizedPattern.length() - 2);
        }

        return normalizedPattern;
    }

    private String joinPaths(String... parts) {
        StringBuilder pathBuilder = new StringBuilder();

        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }

            String normalizedPart = part.trim();
            if (pathBuilder.length() == 0) {
                pathBuilder.append(normalizedPart.startsWith("/") ? normalizedPart : "/" + normalizedPart);
                continue;
            }

            if (pathBuilder.charAt(pathBuilder.length() - 1) != '/') {
                pathBuilder.append('/');
            }

            pathBuilder.append(normalizedPart.startsWith("/") ? normalizedPart.substring(1) : normalizedPart);
        }

        return pathBuilder.length() == 0 ? "/" : pathBuilder.toString();
    }
}
