package com.usedcarrot.auth.service;

import com.usedcarrot.audit.domain.AuditEventType;
import com.usedcarrot.auth.dto.RegisterRequest;
import com.usedcarrot.common.AppException;
import com.usedcarrot.common.AuditLogger;
import com.usedcarrot.common.ErrorCode;
import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserRole;
import com.usedcarrot.user.repository.UserRepository;
import com.usedcarrot.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final AuditLogger auditLogger;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, WalletService walletService,
                       AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.walletService = walletService;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public void register(RegisterRequest request, HttpServletRequest servletRequest) {
        if (!request.passwordMatches()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "이미 사용 중인 닉네임입니다.");
        }
        User user = userRepository.save(new User(
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            request.getNickname(),
            request.getRegion(),
            UserRole.ROLE_USER
        ));
        walletService.createInitialWallet(user, servletRequest);
        auditLogger.log(user.getId(), AuditEventType.REGISTER, "SUCCESS", "user registered", servletRequest);
    }
}
