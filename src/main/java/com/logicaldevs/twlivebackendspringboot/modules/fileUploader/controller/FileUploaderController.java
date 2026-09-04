package com.logicaldevs.twlivebackendspringboot.modules.fileUploader.controller;


import com.logicaldevs.twlivebackendspringboot.constants.ApiPaths;
import com.logicaldevs.twlivebackendspringboot.modules.fileUploader.mappers.UploadFileMapper;
import com.logicaldevs.twlivebackendspringboot.modules.fileUploader.service.FileUploaderService;
import com.logicaldevs.twlivebackendspringboot.responseparser.ResponseWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping(ApiPaths.BASE_API)
@AllArgsConstructor
public class FileUploaderController {

    private final FileUploaderService fileUploaderService;

    @PostMapping(value = ApiPaths.UPLOAD_FILE,consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseWrapper<?>> upload(@Valid @ModelAttribute UploadFileMapper request,
                                                     HttpServletRequest httpRequest) {
        MultipartFile file = request.getFile();
        long start = System.currentTimeMillis();
        log.info("[UPLOAD] request from={} name='{}' size={} bytes contentType={}",
                clientIp(httpRequest),
                file != null ? file.getOriginalFilename() : null,
                file != null ? file.getSize() : -1,
                file != null ? file.getContentType() : null);
        try {
            ResponseWrapper<?> response = fileUploaderService.saveFile(request);
            log.info("[UPLOAD] done name='{}' -> publicPath='{}' in {} ms",
                    file != null ? file.getOriginalFilename() : null, response.getData(), System.currentTimeMillis() - start);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("[UPLOAD] FAILED name='{}' from={} reason={}",
                    file != null ? file.getOriginalFilename() : null, clientIp(httpRequest), e.getMessage());
            throw e;
        }
    }

    @GetMapping(ApiPaths.FILE_PUBLIC_PATH)
    public ResponseEntity<ResponseWrapper<?>> getPublicFilePath(@RequestParam String fileName,
                                                                HttpServletRequest httpRequest) {
        log.info("[GET-FILE] request from={} fileName='{}'", clientIp(httpRequest), fileName);
        try {
            ResponseWrapper<?> response = fileUploaderService.getPublicFilePath(fileName);
            log.info("[GET-FILE] done fileName='{}' -> url='{}'", fileName, response.getData());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("[GET-FILE] FAILED fileName='{}' from={} reason={}", fileName, clientIp(httpRequest), e.getMessage());
            throw e;
        }
    }

    /** Real client IP, honouring X-Forwarded-For set by the IIS/nginx reverse proxy. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

}
