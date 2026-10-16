package com.trackingworld.tw.files;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileUploaderServiceTest {

    @Test
    void publicPathIsUnchangedAfterContextPathRemoval() {
        // production config: no context path, static pattern /technician/images/**
        FileUploaderService service = new FileUploaderService("", "/technician/images/**");
        assertThat(service.buildPublicPath(FileUtils.getPublicFilePath("photo.jpg")))
                .isEqualTo("/technician/images/files/photo.jpg");
    }

    @Test
    void legacyLayoutProducedTheSamePath() {
        // what the old deployment (context-path=/technician, pattern /images/**) produced
        FileUploaderService legacy = new FileUploaderService("/technician", "/images/**");
        FileUploaderService current = new FileUploaderService("", "/technician/images/**");
        String relative = FileUtils.getPublicFilePath("a b.png");
        assertThat(current.buildPublicPath(relative)).isEqualTo(legacy.buildPublicPath(relative));
    }

    @Test
    void defaultPatternFallsBackToRootRelativePath() {
        FileUploaderService service = new FileUploaderService("", "/**");
        assertThat(service.buildPublicPath("files/x.jpg")).isEqualTo("/files/x.jpg");
    }
}
