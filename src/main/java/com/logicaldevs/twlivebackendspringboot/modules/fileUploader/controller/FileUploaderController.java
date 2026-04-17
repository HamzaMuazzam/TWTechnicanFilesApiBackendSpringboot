package com.logicaldevs.twlivebackendspringboot.modules.fileUploader.controller;


import com.logicaldevs.twlivebackendspringboot.constants.ApiPaths;
import com.logicaldevs.twlivebackendspringboot.modules.fileUploader.mappers.UploadFileMapper;
import com.logicaldevs.twlivebackendspringboot.modules.fileUploader.service.FileUploaderService;
import com.logicaldevs.twlivebackendspringboot.responseparser.ResponseWrapper;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE_API)
@AllArgsConstructor
public class FileUploaderController {

    private final FileUploaderService fileUploaderService;

    @PostMapping(value = ApiPaths.UPLOAD_FILE,consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseWrapper<?>> upload(@Valid @ModelAttribute UploadFileMapper request) {
        return  ResponseEntity.ok(fileUploaderService.saveFile(request));
    }

    @GetMapping(ApiPaths.FILE_PUBLIC_PATH)
    public ResponseEntity<ResponseWrapper<?>> getPublicFilePath(@RequestParam String fileName) {
        return ResponseEntity.ok(fileUploaderService.getPublicFilePath(fileName));
    }

}
