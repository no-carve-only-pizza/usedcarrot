package com.usedcarrot.wallet.service;

import com.usedcarrot.audit.domain.AuditEventType;
import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.common.AuditLogger;
import com.usedcarrot.common.ErrorCode;
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
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
    private static final long MAX_ADMIN_GRANT = 1_000_000L;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final AuditLogger auditLogger;
    private ProductService productService;
    private UserService userService;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository transactionRepository,
                         AuditLogger auditLogger) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogger = auditLogger;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setServices(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
    }

    @Transactional
    public Wallet createWallet(User user, HttpServletRequest request) {
        Wallet wallet = walletRepository.save(new Wallet(user, 0L));
        auditLogger.log(user.getId(), AuditEventType.WALLET_CREATED, "SUCCESS", "wallet created", request);
        return wallet;
    }

    @Transactional
    public void adminGrant(Long userId, long amount, CurrentUser admin, HttpServletRequest request) {
        if (amount <= 0 || amount > MAX_ADMIN_GRANT) {
            throw new AppException(ErrorCode.BAD_REQUEST, "지급액은 1~1,000,000 CC여야 합니다.");
        }
        User target = userService.findById(userId);
        Wallet wallet = walletRepository.findWithLockByUserId(userId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "지갑을 찾을 수 없습니다."));
        wallet.deposit(amount);
        transactionRepository.save(new WalletTransaction(null, null, target, null, wallet, amount,
            WalletTransactionType.ADMIN_ADJUSTMENT, WalletTransactionStatus.COMPLETED, UUID.randomUUID().toString(),
            "admin-grant-" + UUID.randomUUID(), null));
        auditLogger.log(admin.getId(), AuditEventType.WALLET_ADMIN_GRANTED, "SUCCESS",
            "targetUserId=" + userId + ",amount=" + amount, request);
    }

    @Transactional(readOnly = true)
    public Wallet myWallet(CurrentUser currentUser) {
        return walletRepository.findByUserId(currentUser.getId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "지갑을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<WalletTransaction> myTransactions(CurrentUser currentUser) {
        return transactionRepository.findMine(currentUser.getId());
    }

    @Transactional
    public WalletTransaction purchase(CurrentUser currentUser, Long productId, String idempotencyKey, HttpServletRequest request) {
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
            .map(existing -> verifyIdempotentReplay(existing, currentUser, productId, request))
            .orElseGet(() -> doPurchase(currentUser, productId, idempotencyKey, request));
    }

    private WalletTransaction doPurchase(CurrentUser currentUser, Long productId, String idempotencyKey, HttpServletRequest request) {
        User buyer = userService.findById(currentUser.getId());
        if (buyer.getStatus() != UserStatus.ACTIVE) {
            fail(currentUser.getId(), "restricted user", request);
        }
        Product product = productService.findForUpdate(productId);
        User seller = product.getSeller();
        if (seller.getId().equals(buyer.getId())) {
            fail(buyer.getId(), "self purchase blocked", request);
        }
        if (!product.isPurchasable()) {
            fail(buyer.getId(), "invalid product status", request);
        }
        Wallet buyerWallet = walletRepository.findWithLockByUserId(buyer.getId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "구매자 지갑을 찾을 수 없습니다."));
        Wallet sellerWallet = walletRepository.findWithLockByUserId(seller.getId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "판매자 지갑을 찾을 수 없습니다."));
        long amount = product.getPrice();
        buyerWallet.withdraw(amount);
        sellerWallet.deposit(amount);
        product.changeStatus(ProductStatus.SOLD);
        WalletTransaction tx = transactionRepository.save(new WalletTransaction(product, buyer, seller, buyerWallet, sellerWallet,
            amount, WalletTransactionType.PRODUCT_PURCHASE, WalletTransactionStatus.COMPLETED, UUID.randomUUID().toString(),
            idempotencyKey, null));
        auditLogger.log(buyer.getId(), AuditEventType.CARROTCOIN_TRANSFER_COMPLETED, "SUCCESS", "productId=" + productId + ",txId=" + tx.getId(), request);
        return tx;
    }

    private WalletTransaction verifyIdempotentReplay(WalletTransaction existing, CurrentUser currentUser, Long productId,
                                                     HttpServletRequest request) {
        boolean sameBuyer = existing.getBuyer() != null && existing.getBuyer().getId().equals(currentUser.getId());
        boolean sameProduct = existing.getProduct() != null && existing.getProduct().getId().equals(productId);
        if (!sameBuyer || !sameProduct) {
            auditLogger.log(currentUser.getId(), AuditEventType.CARROTCOIN_TRANSFER_FAILED, "FAIL", "idempotency key mismatch", request);
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "이미 다른 요청에 사용된 멱등성 키입니다.");
        }
        return existing;
    }

    private void fail(Long userId, String detail, HttpServletRequest request) {
        auditLogger.log(userId, AuditEventType.CARROTCOIN_TRANSFER_FAILED, "FAIL", detail, request);
        throw new AppException(ErrorCode.INVALID_STATE, "CarrotCoin 구매를 처리할 수 없습니다.");
    }
}
