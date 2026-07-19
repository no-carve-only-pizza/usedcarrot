package com.usedcarrot.config;

import com.usedcarrot.product.domain.Product;
import com.usedcarrot.product.domain.ProductImage;
import com.usedcarrot.product.domain.ProductStatus;
import com.usedcarrot.product.repository.ProductRepository;
import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserRole;
import com.usedcarrot.user.repository.UserRepository;
import com.usedcarrot.wallet.repository.WalletRepository;
import com.usedcarrot.wallet.service.WalletService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@Order(100)
public class LocalDemoDataLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LocalDemoDataLoader.class);
    private static final String SELLER_EMAIL = "seller@demo.local";
    private static final String BUYER_EMAIL = "buyer@demo.local";
    private static final String DEMO_PASSWORD = "DemoUser1234!";

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;
    private final Path uploadDir;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public LocalDemoDataLoader(UserRepository userRepository, ProductRepository productRepository,
                               WalletRepository walletRepository, WalletService walletService,
                               PasswordEncoder passwordEncoder,
                               @Value("${usedcarrot.upload-dir}") String uploadDir) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.walletRepository = walletRepository;
        this.walletService = walletService;
        this.passwordEncoder = passwordEncoder;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(SELLER_EMAIL)) {
            backfillMissingImages();
            backfillPayToAddresses();
            return;
        }

        User seller = saveUser(SELLER_EMAIL, "당근판매자", "서울 마포구");
        User seller2 = saveUser("seller2@demo.local", "이웃장터", "서울 강남구");
        User buyer = saveUser(BUYER_EMAIL, "구매연습생", "서울 성동구");

        seedCatalog(seller, seller2);
        log.info("로컬 데모 계정/상품/이미지를 생성했습니다. seller={}/{}, buyer={}/{}",
            SELLER_EMAIL, DEMO_PASSWORD, BUYER_EMAIL, DEMO_PASSWORD);
    }

    private void seedCatalog(User seller, User seller2) {
        // 데모 목록용. 구매하려면 판매자/구매자 모두 MetaMask를 연결해야 함.
        saveProduct(seller, "아이패드 미니 6세대", "케이스·펜슬 포함, 배터리 상태 양호합니다. 직거래 선호.",
            eth("0.001"), "디지털/가전", "서울 마포구", ProductStatus.ON_SALE,
            "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&w=800&q=80");
        saveProduct(seller, "무인양품 원목 책장", "이사로 급처분합니다. 생활 스크래치 약간 있어요.",
            eth("0.0005"), "가구/인테리어", "서울 마포구", ProductStatus.ON_SALE,
            "https://images.unsplash.com/photo-1594620302200-9a762244a156?auto=format&fit=crop&w=800&q=80");
        saveProduct(seller, "나이키 운동화 270", "두 번 신고 보관만 했습니다. 박스 있어요.",
            eth("0.0008"), "스포츠/레저", "서울 마포구", ProductStatus.RESERVED,
            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80");
        saveProduct(seller2, "캠핑 의자 2개 세트", "작년 가을 캠핑 때 사용. 접이식, 가벼움.",
            eth("0.0003"), "스포츠/레저", "서울 강남구", ProductStatus.ON_SALE,
            "https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?auto=format&fit=crop&w=800&q=80");
        saveProduct(seller2, "코스트코 프라이팬", "거의 새것. 코팅 상태 깨끗합니다.",
            eth("0.0002"), "생활/주방", "서울 강남구", ProductStatus.ON_SALE,
            "https://images.unsplash.com/photo-1556910103-1c02745aae4d?auto=format&fit=crop&w=800&q=80");
        saveProduct(seller2, "미드나잇 라이브러리 책", "밑줄 없음. 커버 포함.",
            eth("0.0001"), "도서/취미", "서울 강남구", ProductStatus.ON_SALE,
            "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=800&q=80");
    }

    private long eth(String value) {
        return com.usedcarrot.crypto.EthFormatter.toWei(new java.math.BigDecimal(value));
    }

    private void backfillPayToAddresses() {
        int filled = 0;
        for (Product product : productRepository.findAll()) {
            if (product.getPayToAddress() != null && !product.getPayToAddress().isBlank()) {
                continue;
            }
            String wallet = product.getSeller().getWalletAddress();
            if (wallet == null || wallet.isBlank()) {
                continue;
            }
            product.backfillPayToAddress(wallet);
            filled++;
        }
        if (filled > 0) {
            log.info("기존 상품 {}건에 payToAddress를 채웠습니다.", filled);
        }
    }

    private void backfillMissingImages() {
        List<Product> products = new java.util.ArrayList<>();
        userRepository.findByEmail(SELLER_EMAIL).ifPresent(seller ->
            products.addAll(productRepository.findBySellerIdOrderByCreatedAtDesc(seller.getId())));
        userRepository.findByEmail("seller2@demo.local").ifPresent(seller2 ->
            products.addAll(productRepository.findBySellerIdOrderByCreatedAtDesc(seller2.getId())));

        int filled = 0;
        for (Product product : products) {
            product.getImages().size();
            if (!product.getImages().isEmpty()) {
                continue;
            }
            String url = imageUrlFor(product.getTitle());
            if (url == null) {
                continue;
            }
            try {
                product.addImage(downloadImage(url, slug(product.getTitle()) + ".jpg"));
                filled++;
            } catch (Exception e) {
                log.warn("데모 이미지 다운로드 실패: {} ({})", product.getTitle(), e.getMessage());
            }
        }
        if (filled > 0) {
            log.info("기존 데모 상품 {}개에 이미지를 추가했습니다.", filled);
        } else {
            log.info("로컬 데모 데이터가 이미 있어 건너뜁니다.");
        }
    }

    private String imageUrlFor(String title) {
        if (title.contains("아이패드")) {
            return "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?auto=format&fit=crop&w=800&q=80";
        }
        if (title.contains("책장")) {
            return "https://images.unsplash.com/photo-1594620302200-9a762244a156?auto=format&fit=crop&w=800&q=80";
        }
        if (title.contains("운동화")) {
            return "https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=800&q=80";
        }
        if (title.contains("캠핑")) {
            return "https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?auto=format&fit=crop&w=800&q=80";
        }
        if (title.contains("프라이팬")) {
            return "https://images.unsplash.com/photo-1556910103-1c02745aae4d?auto=format&fit=crop&w=800&q=80";
        }
        if (title.contains("라이브러리") || title.contains("책")) {
            return "https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=800&q=80";
        }
        return null;
    }

    private User saveUser(String email, String nickname, String region) {
        User user = userRepository.save(new User(email, passwordEncoder.encode(DEMO_PASSWORD),
            nickname, region, UserRole.ROLE_USER));
        walletService.createWallet(user, null);
        return user;
    }

    private void saveProduct(User seller, String title, String description, long price,
                             String category, String region, ProductStatus status, String imageUrl) {
        Product product = new Product(seller, title, description, price, category, region);
        product.changeStatus(status);
        try {
            product.addImage(downloadImage(imageUrl, slug(title) + ".jpg"));
        } catch (Exception e) {
            log.warn("데모 이미지 다운로드 실패, 이미지 없이 등록: {} ({})", title, e.getMessage());
        }
        productRepository.save(product);
    }

    private ProductImage downloadImage(String url, String originalName) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("User-Agent", "UsedCarrot-DemoSeeder/1.0")
            .GET()
            .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode());
        }
        byte[] body = response.body();
        if (body.length < 100 || !isJpeg(body) && !isPng(body)) {
            throw new IOException("이미지가 아니거나 파일이 너무 작습니다.");
        }
        boolean jpeg = isJpeg(body);
        String extension = jpeg ? "jpg" : "png";
        String mime = jpeg ? "image/jpeg" : "image/png";
        Files.createDirectories(uploadDir);
        String stored = UUID.randomUUID() + "." + extension;
        Path target = uploadDir.resolve(stored).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new IOException("invalid path");
        }
        Files.write(target, body);
        return new ProductImage(originalName, stored, mime, body.length, target.toString());
    }

    private static boolean isJpeg(byte[] bytes) {
        return bytes.length >= 2 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8;
    }

    private static boolean isPng(byte[] bytes) {
        return bytes.length >= 4
            && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47;
    }

    private static String slug(String title) {
        return title.replaceAll("\\s+", "-").replaceAll("[^\\w가-힣-]", "");
    }
}
