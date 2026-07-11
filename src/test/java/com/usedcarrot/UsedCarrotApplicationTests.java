package com.usedcarrot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.usedcarrot.auth.dto.RegisterRequest;
import com.usedcarrot.auth.service.AuthService;
import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.product.domain.ProductStatus;
import com.usedcarrot.product.dto.ProductCreateRequest;
import com.usedcarrot.product.dto.ProductUpdateRequest;
import com.usedcarrot.product.service.ProductService;
import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserStatus;
import com.usedcarrot.user.repository.UserRepository;
import com.usedcarrot.wallet.repository.WalletRepository;
import com.usedcarrot.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:usedcarrot-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "usedcarrot.upload-dir=./build/test-uploads"
})
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
    WalletService walletService;

    @Test
    void contextLoads() {
        assertThat(userRepository.existsByEmail("admin@usedcarrot.test")).isTrue();
    }

    @Test
    void registerCreatesWalletWithInitialBalance() {
        register("buyer@example.com", "구매자");

        User buyer = userRepository.findByEmail("buyer@example.com").orElseThrow();
        assertThat(walletRepository.findByUserId(buyer.getId()).orElseThrow().getBalance()).isEqualTo(1_000_000L);
    }

    @Test
    void walletPurchaseUsesServerPriceAndBlocksSelfPurchase() {
        register("seller@example.com", "판매자");
        register("buyer2@example.com", "구매자2");
        User seller = userRepository.findByEmail("seller@example.com").orElseThrow();
        User buyer = userRepository.findByEmail("buyer2@example.com").orElseThrow();

        ProductCreateRequest request = new ProductCreateRequest();
        request.setTitle("안전한 자전거");
        request.setPrice(1000L);
        request.setCategory("SPORTS");
        request.setRegion("서울");
        request.setDescription("상태가 좋은 중고 자전거입니다.");

        var product = productService.create(new CurrentUser(seller), request, null, new MockHttpServletRequest());

        walletService.purchase(new CurrentUser(buyer), product.getId(), "purchase-test-key", new MockHttpServletRequest());

        assertThat(walletRepository.findByUserId(buyer.getId()).orElseThrow().getBalance()).isEqualTo(999_000L);
        assertThat(walletRepository.findByUserId(seller.getId()).orElseThrow().getBalance()).isEqualTo(1_001_000L);
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
