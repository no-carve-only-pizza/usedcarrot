package com.usedcarrot.user.service;

import com.usedcarrot.audit.domain.AuditEventType;
import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.common.AuditLogger;
import com.usedcarrot.common.ErrorCode;
import com.usedcarrot.crypto.EthereumService;
import com.usedcarrot.product.domain.ProductStatus;
import com.usedcarrot.product.repository.ProductRepository;
import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserRole;
import com.usedcarrot.user.domain.UserStatus;
import com.usedcarrot.user.dto.PasswordChangeRequest;
import com.usedcarrot.user.dto.ProfileUpdateRequest;
import com.usedcarrot.user.dto.WalletLinkRequest;
import com.usedcarrot.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.EnumSet;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;
    private final EthereumService ethereumService;

    public UserService(UserRepository userRepository, ProductRepository productRepository,
                       PasswordEncoder passwordEncoder, AuditLogger auditLogger,
                       EthereumService ethereumService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogger = auditLogger;
        this.ethereumService = ethereumService;
    }

    @Transactional(readOnly = true)
    public User current(CurrentUser currentUser) {
        return findById(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    @Transactional
    public void updateProfile(CurrentUser currentUser, ProfileUpdateRequest request) {
        User user = findById(currentUser.getId());
        ensureNotDeleted(user);
        if (userRepository.existsByNicknameAndIdNot(request.getNickname(), user.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "이미 사용 중인 닉네임입니다.");
        }
        user.updateProfile(request.getNickname(), request.getRegion(), request.getBio());
    }

    @Transactional
    public void linkWallet(CurrentUser currentUser, WalletLinkRequest request, HttpServletRequest servletRequest) {
        User user = findById(currentUser.getId());
        ensureNotDeleted(user);
        if (user.getWalletAddress() != null && !user.getWalletAddress().isBlank()
            && productRepository.existsBySellerIdAndStatusIn(user.getId(),
                EnumSet.of(ProductStatus.ON_SALE, ProductStatus.RESERVED))) {
            throw new AppException(ErrorCode.INVALID_STATE,
                "판매중/예약중 상품이 있으면 지갑을 바꿀 수 없습니다. 상품을 삭제한 뒤 다시 연결하세요.");
        }
        String message = normalizeWalletLinkMessage(request.getMessage());
        String expectedPrefix = "UsedCarrot wallet link|userId=" + user.getId() + "|nonce=";
        if (!message.startsWith(expectedPrefix)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "지갑 연결 메시지 형식이 올바르지 않습니다.");
        }
        HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "지갑 연결 세션이 없습니다. 새로고침 후 다시 시도하세요.");
        }
        String messageNonce = WalletLinkNonceStore.extractNonce(message);
        WalletLinkNonceStore.requireValid(session, messageNonce);

        String recovered = ethereumService.recoverAddress(message, request.getSignature());
        String claimed = ethereumService.normalizeAddress(request.getAddress());
        if (!recovered.equalsIgnoreCase(claimed)) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "서명 주소와 제출 주소가 일치하지 않습니다.");
        }
        if (userRepository.existsByWalletAddressAndIdNot(claimed, user.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "이미 다른 계정에 연결된 지갑입니다.");
        }
        user.linkWallet(claimed);
        auditLogger.log(user.getId(), AuditEventType.WALLET_LINKED, "SUCCESS", "wallet=" + claimed, servletRequest);
    }

    private static String normalizeWalletLinkMessage(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    @Transactional
    public void changePassword(CurrentUser currentUser, PasswordChangeRequest request, HttpServletRequest servletRequest) {
        if (!request.passwordMatches()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "새 비밀번호가 일치하지 않습니다.");
        }
        User user = findById(currentUser.getId());
        ensureNotDeleted(user);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다.");
        }
        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        auditLogger.log(user.getId(), AuditEventType.PASSWORD_CHANGED, "SUCCESS", "password changed", servletRequest);
    }

    @Transactional
    public void withdraw(CurrentUser currentUser, String currentPassword, HttpServletRequest servletRequest) {
        User user = findById(currentUser.getId());
        if (user.getRole() == UserRole.ROLE_ADMIN) {
            throw new AppException(ErrorCode.BAD_REQUEST, "관리자 계정은 탈퇴할 수 없습니다.");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new AppException(ErrorCode.INVALID_STATE, "이미 탈퇴한 계정입니다.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다.");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        String nickname = ("탈퇴" + user.getId());
        if (nickname.length() > 20) {
            nickname = nickname.substring(0, 20);
        }
        user.withdraw("deleted-" + user.getId() + "-" + token + "@invalid.local", nickname);
        auditLogger.log(user.getId(), AuditEventType.USER_WITHDRAWN, "SUCCESS", "account withdrawn", servletRequest);
    }

    @Transactional
    public void changeStatus(Long userId, UserStatus status, CurrentUser admin, HttpServletRequest servletRequest) {
        if (admin.getId().equals(userId)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "관리자는 본인 계정을 정지할 수 없습니다.");
        }
        User user = findById(userId);
        user.changeStatus(status);
        auditLogger.log(admin.getId(), AuditEventType.USER_STATUS_CHANGED, "SUCCESS",
            "targetUserId=" + userId + ",status=" + status, servletRequest);
    }

    private void ensureNotDeleted(User user) {
        if (user.getStatus() == UserStatus.DELETED) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "탈퇴한 계정입니다.");
        }
    }
}
