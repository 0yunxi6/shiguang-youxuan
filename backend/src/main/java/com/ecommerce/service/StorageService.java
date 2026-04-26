package com.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.entity.MediaAsset;
import com.ecommerce.entity.User;
import com.ecommerce.mapper.MediaAssetMapper;
import com.ecommerce.mapper.UserMapper;
import com.ecommerce.util.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/ogg");

    private final MediaAssetMapper mediaAssetMapper;
    private final UserMapper userMapper;

    @Value("${storage.provider:local}")
    private String provider;

    @Value("${storage.local-dir:${file.upload-dir:uploads}}")
    private String localDir;

    @Value("${storage.public-base-url:}")
    private String publicBaseUrl;

    @Value("${storage.bucket:local}")
    private String bucket;

    public MediaAsset store(MultipartFile file, String bizType) throws IOException {
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        boolean image = IMAGE_TYPES.contains(type);
        boolean video = VIDEO_TYPES.contains(type);
        if (!image && !video) {
            throw new IllegalArgumentException("仅支持 jpg/png/gif/webp 图片或 mp4/webm/ogg 视频");
        }
        if (image && file.getSize() > 10 * 1024 * 1024L) {
            throw new IllegalArgumentException("图片不能超过 10MB");
        }
        if (video && file.getSize() > 50 * 1024 * 1024L) {
            throw new IllegalArgumentException("视频不能超过 50MB");
        }

        String extension = extension(file.getOriginalFilename(), type);
        String datePath = LocalDate.now().toString().replace("-", "/");
        String objectKey = (StringUtils.hasText(bizType) ? sanitizeSegment(bizType) : (image ? "images" : "videos"))
                + "/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + extension;
        String activeProvider = StringUtils.hasText(provider) ? provider.trim().toLowerCase(Locale.ROOT) : "local";

        // 当前基础版完整实现 local provider；R2/OSS/COS/S3 通过统一元数据和配置预留迁移入口。
        if (!"local".equals(activeProvider)) {
            activeProvider = "local";
        }

        Path root = Paths.get(localDir).toAbsolutePath().normalize();
        Path target = root.resolve(objectKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("文件路径不合法");
        }
        Files.createDirectories(target.getParent());
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }

        MediaAsset asset = new MediaAsset();
        asset.setUserId(currentUserId());
        asset.setProvider(activeProvider);
        asset.setBucket(bucket);
        asset.setObjectKey(objectKey.replace("\\", "/"));
        asset.setUrl(buildUrl(asset.getObjectKey()));
        asset.setOriginalName(file.getOriginalFilename());
        asset.setContentType(type);
        asset.setSize(file.getSize());
        asset.setBizType(StringUtils.hasText(bizType) ? bizType : (image ? "image" : "video"));
        asset.setCreateTime(LocalDateTime.now());
        mediaAssetMapper.insert(asset);
        return asset;
    }

    public Result<?> page(int page, int size, String bizType, String providerName, String keyword) {
        page = Math.max(page, 1);
        size = Math.min(Math.max(size, 1), 100);
        QueryWrapper<MediaAsset> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(bizType)) {
            wrapper.eq("biz_type", bizType.trim());
        }
        if (StringUtils.hasText(providerName)) {
            wrapper.eq("provider", providerName.trim());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("original_name", kw)
                    .or().like("object_key", kw)
                    .or().like("url", kw));
        }
        wrapper.orderByDesc("id");
        Page<MediaAsset> result = mediaAssetMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", result.getRecords());
        data.put("total", result.getTotal());
        data.put("current", result.getCurrent());
        data.put("size", result.getSize());
        data.put("pages", result.getPages());
        data.put("config", Map.of(
                "provider", provider,
                "activeProvider", "local",
                "bucket", bucket,
                "publicBaseUrl", publicBaseUrl == null ? "" : publicBaseUrl
        ));
        return Result.success(data);
    }

    private String buildUrl(String objectKey) {
        String normalizedKey = objectKey == null ? "" : objectKey.replace("\\", "/");
        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl.replaceAll("/+$", "") + "/" + normalizedKey;
        }
        return "/api/uploads/" + normalizedKey;
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName()) || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        User user = userMapper.findByUsername(authentication.getName());
        return user == null ? null : user.getId();
    }

    private String sanitizeSegment(String value) {
        return value == null ? "media" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String extension(String filename, String contentType) {
        if (StringUtils.hasText(filename) && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
            if (ext.length() <= 8) {
                return ext;
            }
        }
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "video/webm" -> ".webm";
            case "video/ogg" -> ".ogg";
            default -> ".mp4";
        };
    }
}
