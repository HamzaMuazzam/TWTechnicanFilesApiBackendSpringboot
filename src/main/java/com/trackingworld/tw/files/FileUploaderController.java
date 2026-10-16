package com.trackingworld.tw.files;


import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ResponseWrapper;
import com.trackingworld.tw.common.web.ClientIp;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping(ApiPaths.TECHNICIAN_BASE)
@AllArgsConstructor
@Tag(name = "Files", description = "Legacy technician file upload API")
public class FileUploaderController {

    private final FileUploaderService fileUploaderService;

    @PostMapping(value = ApiPaths.UPLOAD_FILE, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseWrapper<?>> upload(@Valid @ModelAttribute UploadFileMapper request,
                                                     HttpServletRequest httpRequest) {
        MultipartFile file = request.getFile();
        long start = System.currentTimeMillis();
        log.info("[UPLOAD] request from={} name='{}' size={} bytes contentType={}",
                ClientIp.of(httpRequest),
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
                    file != null ? file.getOriginalFilename() : null, ClientIp.of(httpRequest), e.getMessage());
            throw e;
        }
    }

    @GetMapping(ApiPaths.FILE_PUBLIC_PATH)
    public ResponseEntity<ResponseWrapper<?>> getPublicFilePath(@RequestParam String fileName,
                                                                HttpServletRequest httpRequest) {
        log.info("[GET-FILE] request from={} fileName='{}'", ClientIp.of(httpRequest), fileName);
        try {
            ResponseWrapper<?> response = fileUploaderService.getPublicFilePath(fileName);
            log.info("[GET-FILE] done fileName='{}' -> url='{}'", fileName, response.getData());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("[GET-FILE] FAILED fileName='{}' from={} reason={}", fileName, ClientIp.of(httpRequest), e.getMessage());
            throw e;
        }
    }

}
