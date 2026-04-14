package com.example.itfinance.controller;

import com.example.itfinance.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
@CrossOrigin
public class FileController {
    private static final long MAX_UPLOAD_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".webp");

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> upload(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail("请选择要上传的文件");
        }
        if (file.getSize() > MAX_UPLOAD_SIZE) {
            return ApiResponse.fail("文件过大，最大支持 5MB");
        }

        String userDir = System.getProperty("user.dir");
        File uploadDir = new File(userDir, "uploads");
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            return ApiResponse.fail("创建上传目录失败");
        }

        String origin = Objects.requireNonNullElse(file.getOriginalFilename(), "file.jpg");
        String originalName = StringUtils.cleanPath(origin);
        String ext = "";
        int idx = originalName.lastIndexOf('.');
        if (idx >= 0)
            ext = originalName.substring(idx).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            return ApiResponse.fail("仅支持 jpg/jpeg/png/webp 图片上传");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ApiResponse.fail("文件类型非法，请上传图片");
        }

        String fileName = UUID.randomUUID() + ext;
        File dest = new File(uploadDir, fileName);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            return ApiResponse.fail("文件保存失败: " + e.getMessage());
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("fileName", fileName);
        map.put("url", "/uploads/" + fileName);
        map.put("size", file.getSize());
        return ApiResponse.ok("上传成功", map);
    }
}
