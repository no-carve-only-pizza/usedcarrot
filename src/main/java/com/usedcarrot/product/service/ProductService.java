package com.usedcarrot.product.service;

import com.usedcarrot.audit.domain.AuditEventType;
import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.common.AuditLogger;
import com.usedcarrot.common.ErrorCode;
import com.usedcarrot.crypto.EthFormatter;
import com.usedcarrot.product.domain.Product;
import com.usedcarrot.product.domain.ProductCategories;
import com.usedcarrot.product.domain.ProductStatus;
import com.usedcarrot.product.dto.ProductCreateRequest;
import com.usedcarrot.product.dto.ProductSearchCondition;
import com.usedcarrot.product.dto.ProductUpdateRequest;
import com.usedcarrot.product.repository.ProductRepository;
import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserStatus;
import com.usedcarrot.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final AuditLogger auditLogger;

    public ProductService(ProductRepository productRepository, UserService userService,
                          FileStorageService fileStorageService, AuditLogger auditLogger) {
        this.productRepository = productRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.auditLogger = auditLogger;
    }

    @Transactional(readOnly = true)
    public Page<Product> search(ProductSearchCondition condition, int page, int size) {
        ProductStatus status = normalizePublicStatus(condition.getStatus());
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> products = productRepository.searchVisible(blankToNull(condition.getKeyword()), blankToNull(condition.getCategory()),
            status, blankToNull(condition.getRegion()), pageable);
        initializeImages(products.getContent());
        return products;
    }

    @Transactional(readOnly = true)
    public List<Product> latest() {
        List<Product> products = productRepository.findLatestVisible(PageRequest.of(0, 20));
        initializeImages(products);
        return products;
    }

    @Transactional
    public Product create(CurrentUser currentUser, ProductCreateRequest request, List<MultipartFile> images,
                          HttpServletRequest servletRequest) {
        User seller = userService.findById(currentUser.getId());
        ensureActive(seller);
        if (seller.getWalletAddress() == null || seller.getWalletAddress().isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "상품을 등록하려면 먼저 MetaMask 지갑을 연결하세요.");
        }
        validateCategory(request.getCategory());
        long priceWei = EthFormatter.toWei(request.getPriceEth());
        Product product = new Product(seller, request.getTitle(), request.getDescription(), priceWei,
            request.getCategory(), request.getRegion());
        fileStorageService.store(images, seller.getId(), servletRequest).forEach(product::addImage);
        Product saved = productRepository.save(product);
        auditLogger.log(seller.getId(), AuditEventType.PRODUCT_CREATED, "SUCCESS", "productId=" + saved.getId(), servletRequest);
        return saved;
    }

    @Transactional
    public Product detail(Long id) {
        Product product = findWithSellerAndImages(id);
        if (!product.isVisible()) {
            throw new AppException(ErrorCode.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
        product.increaseViewCount();
        return product;
    }

    @Transactional(readOnly = true)
    public Product find(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Product findWithSellerAndImages(Long id) {
        return productRepository.findByIdWithSellerAndImages(id)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    @Transactional
    public Product findForUpdate(Long id) {
        return productRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Product findForEdit(Long id, CurrentUser currentUser) {
        Product product = findWithSellerAndImages(id);
        ensureOwnerOrAdmin(product, currentUser);
        if (!isAdmin(currentUser) && !product.getStatus().isSellerEditable()) {
            throw new AppException(ErrorCode.INVALID_STATE, "판매중/예약중 상품만 수정할 수 있습니다.");
        }
        return product;
    }

    @Transactional(readOnly = true)
    public List<Product> myProducts(CurrentUser currentUser) {
        List<Product> products = productRepository.findBySellerIdOrderByCreatedAtDesc(currentUser.getId());
        initializeImages(products);
        return products;
    }

    @Transactional
    public void update(Long productId, CurrentUser currentUser, ProductUpdateRequest request,
                       List<MultipartFile> images, HttpServletRequest servletRequest) {
        Product product = findWithSellerAndImages(productId);
        ensureOwnerOrAdmin(product, currentUser);
        ensureOwnerCanMutate(currentUser);
        validateCategory(request.getCategory());
        try {
            long priceWei = EthFormatter.toWei(request.getPriceEth());
            if (isAdmin(currentUser)) {
                product.update(request.getTitle(), request.getDescription(), priceWei,
                    request.getCategory(), request.getRegion(), request.getStatus());
            } else {
                product.updateBySeller(request.getTitle(), request.getDescription(), priceWei,
                    request.getCategory(), request.getRegion(), request.getStatus());
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new AppException(ErrorCode.BAD_REQUEST, e.getMessage());
        }
        appendImages(product, images, currentUser.getId(), servletRequest);
        auditLogger.log(currentUser.getId(), AuditEventType.PRODUCT_UPDATED, "SUCCESS", "productId=" + productId, servletRequest);
    }

    @Transactional
    public void delete(Long productId, CurrentUser currentUser, HttpServletRequest servletRequest) {
        Product product = find(productId);
        ensureOwnerOrAdmin(product, currentUser);
        ensureOwnerCanMutate(currentUser);
        if (!isAdmin(currentUser) && product.getStatus() == ProductStatus.DELETED) {
            throw new AppException(ErrorCode.INVALID_STATE, "이미 삭제된 상품입니다.");
        }
        product.changeStatus(ProductStatus.DELETED);
        auditLogger.log(currentUser.getId(), AuditEventType.PRODUCT_DELETED, "SUCCESS", "productId=" + productId, servletRequest);
    }

    @Transactional
    public void adminStatus(Long productId, ProductStatus status, CurrentUser admin, HttpServletRequest servletRequest) {
        Product product = find(productId);
        product.changeStatus(status);
        auditLogger.log(admin.getId(), AuditEventType.PRODUCT_STATUS_CHANGED, "SUCCESS", "productId=" + productId + ",status=" + status, servletRequest);
    }

    private void appendImages(Product product, List<MultipartFile> images, Long userId, HttpServletRequest servletRequest) {
        if (images == null || images.stream().allMatch(MultipartFile::isEmpty)) {
            return;
        }
        int remaining = product.remainingImageSlots();
        if (remaining <= 0) {
            throw new AppException(ErrorCode.FILE_UPLOAD_REJECTED, "상품당 이미지는 최대 5장까지 등록할 수 있습니다.");
        }
        List<MultipartFile> candidates = images.stream().filter(file -> !file.isEmpty()).toList();
        if (candidates.size() > remaining) {
            throw new AppException(ErrorCode.FILE_UPLOAD_REJECTED, "남은 이미지 슬롯은 " + remaining + "장입니다.");
        }
        fileStorageService.store(candidates, userId, servletRequest).forEach(product::addImage);
    }

    private void validateCategory(String category) {
        if (!ProductCategories.isAllowed(category)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "허용되지 않는 카테고리입니다.");
        }
    }

    private ProductStatus normalizePublicStatus(ProductStatus status) {
        if (status == null) {
            return null;
        }
        if (!status.isPublicFilterable()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "검색할 수 없는 상품 상태입니다.");
        }
        return status;
    }

    private void ensureOwnerOrAdmin(Product product, CurrentUser currentUser) {
        if (!isAdmin(currentUser) && !product.isSeller(currentUser.getId())) {
            throw new AccessDeniedException("forbidden");
        }
    }

    private void ensureOwnerCanMutate(CurrentUser currentUser) {
        if (!isAdmin(currentUser)) {
            ensureActive(userService.findById(currentUser.getId()));
        }
    }

    private boolean isAdmin(CurrentUser currentUser) {
        return currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private void ensureActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "제한된 사용자는 이 기능을 사용할 수 없습니다.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void initializeImages(List<Product> products) {
        products.forEach(product -> product.getImages().size());
    }
}
