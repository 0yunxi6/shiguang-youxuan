package com.ecommerce.controller;

import com.ecommerce.util.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class FileUploadController {
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping("/image")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            return Result.error("图片不能超过 10MB");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return Result.error("不支持的图片类型");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error("不支持的文件格式，仅支持 jpg/png/gif/webp");
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path target = uploadPath.resolve(filename).normalize();
        if (!target.startsWith(uploadPath)) {
            return Result.error("文件路径不合法");
        }
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        String url = "/api/uploads/" + filename;
        return Result.success("上传成功", url);
    }
}
