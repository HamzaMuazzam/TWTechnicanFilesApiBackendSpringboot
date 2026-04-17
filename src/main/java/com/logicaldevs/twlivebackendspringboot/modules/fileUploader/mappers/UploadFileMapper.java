package com.logicaldevs.twlivebackendspringboot.modules.fileUploader.mappers;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class UploadFileMapper {
    @NotNull
    MultipartFile file;
}
