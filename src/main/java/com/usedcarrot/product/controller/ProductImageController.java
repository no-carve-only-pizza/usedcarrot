package com.usedcarrot.product.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.common.ErrorCode;
import com.usedcarrot.product.domain.Product;
import com.usedcarrot.product.domain.ProductImage;
import com.usedcarrot.product.repository.ProductImageRepository;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProductImageController {
    private final ProductImageRepository productImageRepository;

    public ProductImageController(ProductImageRepository productImageRepository) {
        this.productImageRepository = productImageRepository;
    }

    @GetMapping("/uploads/{storedFileName:.+}")
    public ResponseEntity<Resource> image(@PathVariable String storedFileName,
                                          @AuthenticationPrincipal CurrentUser currentUser) {
        ProductImage image = productImageRepository.findByStoredFileNameWithProduct(storedFileName)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "이미지를 찾을 수 없습니다."));
        Product product = image.getProduct();
        if (!canView(product, currentUser)) {
            throw new AppException(ErrorCode.NOT_FOUND, "이미지를 찾을 수 없습니다.");
        }
        Resource resource = new FileSystemResource(Path.of(image.getPath()));
        if (!resource.exists() || !resource.isReadable()) {
            throw new AppException(ErrorCode.NOT_FOUND, "이미지를 찾을 수 없습니다.");
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.getMimeType()))
            .body(resource);
    }

    private boolean canView(Product product, CurrentUser currentUser) {
        if (product.isVisible()) {
            return true;
        }
        if (currentUser == null) {
            return false;
        }
        boolean admin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return admin || product.isSeller(currentUser.getId());
    }
}
