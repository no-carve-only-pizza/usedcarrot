package com.usedcarrot.audit.controller;

import com.usedcarrot.audit.repository.AuditLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminAuditLogController {
    private final AuditLogRepository auditLogRepository;

    public AdminAuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/admin/audit-logs")
    public String logs(Model model) {
        model.addAttribute("logs", auditLogRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, 200)));
        return "admin/audit-logs";
    }
}
