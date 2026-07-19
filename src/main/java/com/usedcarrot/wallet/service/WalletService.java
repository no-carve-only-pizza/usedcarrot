package com.usedcarrot.wallet.service;

import com.usedcarrot.audit.domain.AuditEventType;
import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.common.AuditLogger;
import com.usedcarrot.common.ErrorCode;
import com.usedcarrot.crypto.EthereumService;
import com.usedcarrot.product.domain.Product;
import com.usedcarrot.product.domain.ProductStatus;
import com.usedcarrot.product.service.ProductService;
import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserStatus;
import com.usedcarrot.user.service.UserService;
import com.usedcarrot.wallet.domain.Wallet;
import com.usedcarrot.wallet.domain.WalletTransaction;
import com.usedcarrot.wallet.domain.WalletTransactionStatus;
import com.usedcarrot.wallet.domain.WalletTransactionType;
import com.usedcarrot.wallet.repository.WalletRepository;
import com.usedcarrot.wallet.repository.WalletTransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Keys;

@Service
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final AuditLogger auditLogger;
    private final EthereumService ethereumService;
    private ProductService productService;
    private UserService userService;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository transactionRepository,
                         AuditLogger auditLogger, EthereumService ethereumService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogger = auditLogger;
        this.ethereumService = ethereumService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setServices(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    @Transactional
    public Wallet createWallet(User user, HttpServletRequest request) {
        Wallet wallet = walletRepository.save(new Wallet(user, 0L));
        auditLogger.log(user.getId(), AuditEventType.WALLET_CREATED, "SUCCESS", "payment record created", request);
        return wallet;
    }

    @Transactional(readOnly = true)
    public Wallet myWallet(CurrentUser currentUser) {
        return walletRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "결제 기록을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<WalletTransaction> myTransactions(CurrentUser currentUser) {
        return transactionRepository.findMine(currentUser.getId());
    }

    /**
     * MetaMask로 판매자 주소에 ETH를 보낸 뒤, 서버가 온체인 영수증을 검증하고 상품을 SOLD 처리한다.
     * DB 잔액 차감은 하지 않는다. Wallet/WalletTransaction은 결제 기록용이다.
     */
    @Transactional
    public WalletTransaction confirmOnChainPurchase(CurrentUser currentUser, Long productId, String txHash,
                                                    HttpServletRequest request) {
        String normalizedHash = ethereumService.normalizeTxHash(txHash);
        return transactionRepository.findByTransactionHash(normalizedHash)
            .map(existing -> verifyReplay(existing, currentUser, productId, request))
            .orElseGet(() -> doConfirm(currentUser, productId, normalizedHash, request));
    }

    private WalletTransaction doConfirm(CurrentUser currentUser, Long productId, String txHash,
                                        HttpServletRequest request) {
        User buyer = userService.findById(currentUser.getId());
        if (buyer.getStatus() != UserStatus.ACTIVE) {
            fail(currentUser.getId(), "restricted user", request);
        }
        if (buyer.getWalletAddress() == null || buyer.getWalletAddress().isBlank()) {
            fail(buyer.getId(), "buyer wallet missing", request);
            throw new AppException(ErrorCode.BAD_REQUEST, "구매하려면 MetaMask 지갑을 먼저 연결하세요.");
        }
        Product product = productService.findForUpdate(productId);
        User seller = product.getSeller();
        if (seller.getId().equals(buyer.getId())) {
            fail(buyer.getId(), "self purchase blocked", request);
        }
        if (seller.getWalletAddress() == null || seller.getWalletAddress().isBlank()) {
            fail(buyer.getId(), "seller wallet missing", request);
            throw new AppException(ErrorCode.INVALID_STATE, "판매자가 지갑을 연결하지 않아 구매할 수 없습니다.");
        }
        String payTo = product.getPayToAddress();
        if (payTo == null || payTo.isBlank()) {
            fail(buyer.getId(), "payTo missing", request);
            throw new AppException(ErrorCode.INVALID_STATE, "상품 결제 주소가 없습니다.");
        }
        if (!product.isPurchasable()) {
            fail(buyer.getId(), "invalid product status", request);
        }

        EthereumService.OnChainPayment payment;
        try {
            payment = ethereumService.loadPayment(txHash);
        } catch (AppException e) {
            auditLogger.log(buyer.getId(), AuditEventType.ONCHAIN_PAYMENT_FAILED, "FAIL", e.getMessage(), request);
            throw e;
        }

        if (!equalsAddress(payment.from(), buyer.getWalletAddress())) {
            fail(buyer.getId(), "from mismatch", request);
            throw new AppException(ErrorCode.ACCESS_DENIED, "트랜잭션 송신 주소가 연결된 구매자 지갑과 다릅니다.");
        }
        if (!equalsAddress(payment.to(), payTo)) {
            fail(buyer.getId(), "to mismatch", request);
            throw new AppException(ErrorCode.ACCESS_DENIED, "트랜잭션 수신 주소가 상품 결제 주소와 다릅니다.");
        }
        if (!com.usedcarrot.crypto.PaymentMemo.matches(payment.input(), product.getId())) {
            fail(buyer.getId(), "memo mismatch", request);
            throw new AppException(ErrorCode.ACCESS_DENIED, "이 트랜잭션은 해당 상품 결제용이 아닙니다.");
        }
        BigInteger expected = BigInteger.valueOf(product.getPrice());
        if (payment.valueWei().compareTo(expected) < 0) {
            fail(buyer.getId(), "insufficient value", request);
            throw new AppException(ErrorCode.INVALID_STATE, "온체인 송금액이 상품 가격보다 작습니다.");
        }

        product.changeStatus(ProductStatus.SOLD);
        Wallet buyerLedger = walletRepository.findWithLockByUserId(buyer.getId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "구매자 결제 기록을 찾을 수 없습니다."));
        Wallet sellerLedger = walletRepository.findWithLockByUserId(seller.getId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "판매자 결제 기록을 찾을 수 없습니다."));
        // 결제 내역 저장용 — 실제 ETH는 체인에 이미 이동됨
        sellerLedger.deposit(product.getPrice());

        WalletTransaction tx = transactionRepository.save(new WalletTransaction(
            product, buyer, seller, buyerLedger, sellerLedger, product.getPrice(),
            WalletTransactionType.PRODUCT_PURCHASE, WalletTransactionStatus.COMPLETED,
            payment.txHash(), payment.txHash(), null));
        auditLogger.log(buyer.getId(), AuditEventType.ONCHAIN_PAYMENT_COMPLETED, "SUCCESS",
            "productId=" + productId + ",tx=" + payment.txHash(), request);
        return tx;
    }

    private WalletTransaction verifyReplay(WalletTransaction existing, CurrentUser currentUser, Long productId,
                                           HttpServletRequest request) {
        boolean sameBuyer = existing.getBuyer() != null && existing.getBuyer().getId().equals(currentUser.getId());
        boolean sameProduct = existing.getProduct() != null && existing.getProduct().getId().equals(productId);
        if (!sameBuyer || !sameProduct) {
            auditLogger.log(currentUser.getId(), AuditEventType.ONCHAIN_PAYMENT_FAILED, "FAIL",
                "tx hash reuse mismatch", request);
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "이미 다른 결제에 사용된 트랜잭션입니다.");
        }
        return existing;
    }

    private boolean equalsAddress(String left, String right) {
        return Keys.toChecksumAddress(left).equalsIgnoreCase(Keys.toChecksumAddress(right));
    }

    private void fail(Long userId, String detail, HttpServletRequest request) {
        auditLogger.log(userId, AuditEventType.ONCHAIN_PAYMENT_FAILED, "FAIL", detail, request);
        throw new AppException(ErrorCode.INVALID_STATE, "온체인 구매를 확정할 수 없습니다.");
    }
}
