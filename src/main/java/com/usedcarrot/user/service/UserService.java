package com.usedcarrot.user.service;

import com.usedcarrot.audit.domain.AuditEventType;
import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.common.AuditLogger;
import com.usedcarrot.common.ErrorCode;
import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserStatus;
import com.usedcarrot.user.dto.PasswordChangeRequest;
import com.usedcarrot.user.dto.ProfileUpdateRequest;
import com.usedcarrot.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditLogger auditLogger) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogger = auditLogger;
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
        if (userRepository.existsByNicknameAndIdNot(request.getNickname(), user.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "이미 사용 중인 닉네임입니다.");
        }
        user.updateProfile(request.getNickname(), request.getRegion(), request.getBio());
    }

    @Transactional
    public void changePassword(CurrentUser currentUser, PasswordChangeRequest request, HttpServletRequest servletRequest) {
        if (!request.passwordMatches()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "새 비밀번호가 일치하지 않습니다.");
        }
        User user = findById(currentUser.getId());
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다.");
        }
        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        auditLogger.log(user.getId(), AuditEventType.PASSWORD_CHANGED, "SUCCESS", "password changed", servletRequest);
    }

    @Transactional
    public void changeStatus(Long userId, UserStatus status, CurrentUser admin, HttpServletRequest servletRequest) {
        if (admin.getId().equals(userId)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "관리자는 본인 계정을 정지할 수 없습니다.");
        }
        User user = findById(userId);
        user.changeStatus(status);
        auditLogger.log(admin.getId(), AuditEventType.USER_STATUS_CHANGED, "SUCCESS", "targetUserId=" + userId + ",status=" + status, servletRequest);
    }
}
