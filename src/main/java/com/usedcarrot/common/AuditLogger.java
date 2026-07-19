package com.usedcarrot.common;

import com.usedcarrot.audit.domain.AuditEventType;
import com.usedcarrot.audit.domain.AuditLog;
import com.usedcarrot.audit.repository.AuditLogRepository;
import com.usedcarrot.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditLogger {
    private final AuditLogRepository auditLogRepository;
    private final ClientIpResolver clientIpResolver;

    public AuditLogger(AuditLogRepository auditLogRepository, ClientIpResolver clientIpResolver) {
        this.auditLogRepository = auditLogRepository;
        this.clientIpResolver = clientIpResolver;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, AuditEventType type, String result, String detail, HttpServletRequest request) {
        String safeDetail = detail == null ? null : detail.replaceAll("(?i)(password|token|session|secret)[^,\\s]*", "[redacted]");
        auditLogRepository.save(new AuditLog(userId, type, request == null ? null : clientIpResolver.resolve(request), result, safeDetail));
    }
}
