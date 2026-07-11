package com.usedcarrot.common;

import com.usedcarrot.audit.domain.AuditEventType;
import com.usedcarrot.audit.domain.AuditLog;
import com.usedcarrot.audit.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditLogger {
    private final AuditLogRepository auditLogRepository;

    public AuditLogger(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long userId, AuditEventType type, String result, String detail, HttpServletRequest request) {
        String safeDetail = detail == null ? null : detail.replaceAll("(?i)(password|token|session|secret)[^,\\s]*", "[redacted]");
        auditLogRepository.save(new AuditLog(userId, type, clientIp(request), result, safeDetail));
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
