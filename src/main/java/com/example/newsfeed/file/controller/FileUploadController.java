package com.example.newsfeed.file.controller;

import com.example.newsfeed.constants.config.FileUploadConfig;
import com.example.newsfeed.constants.response.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/files")
public class FileUploadController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_SIZE = 10L * 1024 * 1024; // 10MB

    @PostMapping("/image")
    public ResponseEntity<CommonResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>("파일이 비어있습니다.", null));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>("이미지 파일만 업로드 가능합니다.", null));
        }

        if (file.getSize() > MAX_SIZE) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(new CommonResponse<>("파일 크기는 10MB 이하여야 합니다.", null));
        }

        try {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            Path baseDir = Paths.get(FileUploadConfig.UPLOAD_DIR, dateDir).toAbsolutePath().normalize();
            Files.createDirectories(baseDir);

            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.')).toLowerCase();
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;

            Path target = baseDir.resolve(filename);
            file.transferTo(target.toFile());

            String url = "/uploads/" + dateDir + "/" + filename;
            log.info("이미지 업로드 성공: {}", url);

            return ResponseEntity.ok(new CommonResponse<>("이미지 업로드 완료", Map.of("url", url)));
        } catch (IOException e) {
            log.error("이미지 업로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>("업로드 실패: " + e.getMessage(), null));
        }
    }
}
