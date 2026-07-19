package com.usedcarrot.product.service;

import com.usedcarrot.audit.domain.AuditEventType;
import com.usedcarrot.common.AppException;
import com.usedcarrot.common.AuditLogger;
import com.usedcarrot.common.ErrorCode;
import com.usedcarrot.product.domain.ProductImage;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private static final long MAX_SIZE = 5 * 1024 * 1024L;
    private static final long MAX_PIXELS = 20_000_000L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png");
    private final Path uploadDir;
    private final AuditLogger auditLogger;

    public FileStorageService(@Value("${usedcarrot.upload-dir}") String uploadDir, AuditLogger auditLogger) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.auditLogger = auditLogger;
    }

    public List<ProductImage> store(List<MultipartFile> files, Long userId, HttpServletRequest request) {
        if (files == null) {
            return List.of();
        }
        List<MultipartFile> realFiles = files.stream().filter(file -> !file.isEmpty()).toList();
        if (realFiles.size() > 5) {
            reject(userId, "too many files", request);
        }
        return realFiles.stream().map(file -> storeOne(file, userId, request)).toList();
    }

    private ProductImage storeOne(MultipartFile file, Long userId, HttpServletRequest request) {
        String original = file.getOriginalFilename() == null ? "image" : Path.of(file.getOriginalFilename()).getFileName().toString();
        String extension = extension(original);
        String mime = file.getContentType();
        if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_MIME.contains(mime) || !expectedMime(extension).equals(mime)
            || file.getSize() > MAX_SIZE) {
            reject(userId, "invalid image upload", request);
        }
        try (InputStream inputStream = file.getInputStream()) {
            if (!hasValidSignature(inputStream, extension)) {
                reject(userId, "invalid image signature", request);
            }
            validateDecodableImage(file, extension, userId, request);
            Files.createDirectories(uploadDir);
            String stored = UUID.randomUUID() + "." + extension;
            Path target = uploadDir.resolve(stored).normalize();
            if (!target.startsWith(uploadDir)) {
                reject(userId, "invalid path", request);
            }
            file.transferTo(target);
            return new ProductImage(original, stored, mime, file.getSize(), target.toString());
        } catch (IOException e) {
            throw new AppException(ErrorCode.FILE_UPLOAD_REJECTED, "이미지 저장에 실패했습니다.");
        }
    }

    private boolean hasValidSignature(InputStream inputStream, String extension) throws IOException {
        byte[] bytes = inputStream.readNBytes(16);
        if (bytes.length < 4) {
            return false;
        }
        boolean jpg = (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8;
        boolean png = bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
        return switch (extension) {
            case "jpg", "jpeg" -> jpg;
            case "png" -> png;
            default -> false;
        };
    }

    private String extension(String original) {
        int dot = original.lastIndexOf('.');
        return dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String expectedMime(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            default -> "";
        };
    }

    private void validateDecodableImage(MultipartFile file, String extension, Long userId, HttpServletRequest request) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                || (long) image.getWidth() * image.getHeight() > MAX_PIXELS) {
                reject(userId, "invalid image structure", request);
            }
        } catch (IOException e) {
            reject(userId, "invalid image structure", request);
        }
    }

    private void reject(Long userId, String detail, HttpServletRequest request) {
        auditLogger.log(userId, AuditEventType.FILE_UPLOAD_REJECTED, "FAIL", detail, request);
        throw new AppException(ErrorCode.FILE_UPLOAD_REJECTED, "허용되지 않는 이미지 파일입니다.");
    }
}
