package com.usedcarrot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.usedcarrot.auth.dto.RegisterRequest;
import com.usedcarrot.auth.service.AuthService;
import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.product.domain.ProductStatus;
import com.usedcarrot.product.dto.ProductCreateRequest;
import com.usedcarrot.product.dto.ProductUpdateRequest;
import com.usedcarrot.product.service.ProductService;
import com.usedcarrot.product.service.FileStorageService;
import com.usedcarrot.report.domain.ReportReason;
import com.usedcarrot.report.domain.ReportStatus;
import com.usedcarrot.report.domain.ReportTargetType;
import com.usedcarrot.report.dto.ReportCreateRequest;
import com.usedcarrot.report.repository.ReportRepository;
import com.usedcarrot.report.service.ReportService;
import com.usedcarrot.security.ClientIpResolver;
import com.usedcarrot.security.LoginRateLimitService;
import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserRole;
import com.usedcarrot.user.domain.UserStatus;
import com.usedcarrot.user.repository.UserRepository;
import com.usedcarrot.wallet.repository.WalletRepository;
import com.usedcarrot.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:usedcarrot-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "usedcarrot.upload-dir=./build/test-uploads"
})
@AutoConfigureMockMvc
@Transactional
class UsedCarrotApplicationTests {
    @Autowired
    AuthService authService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    WalletRepository walletRepository;

    @Autowired
    ProductService productService;

    @Autowired
    FileStorageService fileStorageService;

    @Autowired
    WalletService walletService;

    @Autowired
    ReportService reportService;

    @Autowired
    ReportRepository reportRepository;

    @Autowired
    LoginRateLimitService loginRateLimitService;

    @Autowired
    ClientIpResolver clientIpResolver;

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertThat(userRepository.existsByEmail("admin@usedcarrot.test")).isFalse();
    }

    @Test
    void registerCreatesEmptyWallet() {
        register("buyer@example.com", "구매자");

        User buyer = userRepository.findByEmail("buyer@example.com").orElseThrow();
        assertThat(walletRepository.findByUserId(buyer.getId()).orElseThrow().getBalance()).isZero();
    }

    @Test
    void walletPurchaseUsesServerPriceAndBlocksSelfPurchase() {
        register("seller@example.com", "판매자");
        register("buyer2@example.com", "구매자2");
        User seller = userRepository.findByEmail("seller@example.com").orElseThrow();
        User buyer = userRepository.findByEmail("buyer2@example.com").orElseThrow();
        User admin = userRepository.save(new User("secure-admin@example.com", "unused", "보안관리자", "서울", UserRole.ROLE_ADMIN));
        walletService.createWallet(admin, new MockHttpServletRequest());
        walletService.adminGrant(buyer.getId(), 1_000_000L, new CurrentUser(admin), new MockHttpServletRequest());

        ProductCreateRequest request = new ProductCreateRequest();
        request.setTitle("안전한 자전거");
        request.setPrice(1000L);
        request.setCategory("SPORTS");
        request.setRegion("서울");
        request.setDescription("상태가 좋은 중고 자전거입니다.");

        var product = productService.create(new CurrentUser(seller), request, null, new MockHttpServletRequest());

        walletService.purchase(new CurrentUser(buyer), product.getId(), "purchase-test-key", new MockHttpServletRequest());

        assertThat(walletRepository.findByUserId(buyer.getId()).orElseThrow().getBalance()).isEqualTo(999_000L);
        assertThat(walletRepository.findByUserId(seller.getId()).orElseThrow().getBalance()).isEqualTo(1_000L);
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD);
        assertThatThrownBy(() -> walletService.purchase(new CurrentUser(seller), product.getId(), "self-test-key", new MockHttpServletRequest()))
            .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> walletService.purchase(new CurrentUser(seller), product.getId(), "purchase-test-key", new MockHttpServletRequest()))
            .isInstanceOf(AppException.class);
    }

    @Test
    void limitedSellerCannotUpdateOwnProduct() {
        register("limited@example.com", "제한판매자");
        User seller = userRepository.findByEmail("limited@example.com").orElseThrow();

        ProductCreateRequest create = new ProductCreateRequest();
        create.setTitle("판매 상품");
        create.setPrice(500L);
        create.setCategory("ETC");
        create.setRegion("서울");
        create.setDescription("제한 전 등록한 정상 상품입니다.");
        var product = productService.create(new CurrentUser(seller), create, null, new MockHttpServletRequest());

        seller.changeStatus(UserStatus.LIMITED);
        ProductUpdateRequest update = new ProductUpdateRequest();
        update.setTitle("수정 시도");
        update.setPrice(500L);
        update.setCategory("ETC");
        update.setRegion("서울");
        update.setDescription("제한 사용자가 수정하려는 설명입니다.");
        update.setStatus(ProductStatus.ON_SALE);

        assertThatThrownBy(() -> productService.update(product.getId(), new CurrentUser(seller), update, new MockHttpServletRequest()))
            .isInstanceOf(AppException.class);
    }

    @Test
    void reportsRequireAdminApprovalBeforeProductIsHidden() {
        register("report-seller@example.com", "신고판매자");
        User seller = userRepository.findByEmail("report-seller@example.com").orElseThrow();
        var productRequest = new ProductCreateRequest();
        productRequest.setTitle("신고 검증 상품");
        productRequest.setPrice(1000L);
        productRequest.setCategory("ETC");
        productRequest.setRegion("서울");
        productRequest.setDescription("신고 승인 흐름을 검증하는 정상 상품입니다.");
        var product = productService.create(new CurrentUser(seller), productRequest, null, new MockHttpServletRequest());

        for (int i = 1; i <= 3; i++) {
            register("reporter" + i + "@example.com", "신고자" + i);
            User reporter = userRepository.findByEmail("reporter" + i + "@example.com").orElseThrow();
            ReportCreateRequest report = new ReportCreateRequest();
            report.setTargetType(ReportTargetType.PRODUCT);
            report.setTargetId(product.getId());
            report.setReason(ReportReason.FAKE_INFO);
            reportService.create(new CurrentUser(reporter), report, new MockHttpServletRequest());
        }
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);

        User admin = userRepository.save(new User("report-admin@example.com", "unused", "신고관리자", "서울", UserRole.ROLE_ADMIN));
        reportRepository.findAll().forEach(report -> reportService.process(
            report.getId(), ReportStatus.RESOLVED, "검토 완료", new CurrentUser(admin), new MockHttpServletRequest()));
        assertThat(product.getStatus()).isEqualTo(ProductStatus.HIDDEN);
    }

    @Test
    void loginRateLimitIsPerIpAndForwardedHeaderIsIgnoredByDefault() {
        String attackerIp = "198.51.100.44";
        for (int i = 0; i < 10; i++) {
            loginRateLimitService.recordFailure(attackerIp);
        }
        assertThat(loginRateLimitService.isBlocked(attackerIp)).isTrue();
        assertThat(loginRateLimitService.isBlocked("198.51.100.45")).isFalse();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(attackerIp);
        request.addHeader("X-Forwarded-For", "203.0.113.77");
        assertThat(clientIpResolver.resolve(request)).isEqualTo(attackerIp);
    }

    @Test
    void productWriteFormRequiresAuthenticationAndMissingProductReturns404() throws Exception {
        mockMvc.perform(get("/products/new"))
            .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/products/999999"))
            .andExpect(status().isNotFound());
    }

    @Test
    void fileUploadRejectsPngSignatureWithoutDecodableImageData() {
        byte[] fakePng = new byte[] {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x48, 0x44, 0x52
        };
        MockMultipartFile file = new MockMultipartFile("images", "fake.png", "image/png", fakePng);

        assertThatThrownBy(() -> fileStorageService.store(java.util.List.of(file), 1L, new MockHttpServletRequest()))
            .isInstanceOf(AppException.class);
    }

    private void register(String email, String nickname) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("Password123!");
        request.setPasswordConfirm("Password123!");
        request.setNickname(nickname);
        request.setRegion("서울");
        authService.register(request, new MockHttpServletRequest());
    }
}
