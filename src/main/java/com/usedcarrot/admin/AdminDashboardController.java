package com.usedcarrot.admin;

import com.usedcarrot.audit.repository.AuditLogRepository;
import com.usedcarrot.product.domain.ProductStatus;
import com.usedcarrot.product.repository.ProductRepository;
import com.usedcarrot.report.domain.ReportStatus;
import com.usedcarrot.report.repository.ReportRepository;
import com.usedcarrot.user.domain.UserStatus;
import com.usedcarrot.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminDashboardController(UserRepository userRepository, ProductRepository productRepository,
                                    ReportRepository reportRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.reportRepository = reportRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("activeUserCount", userRepository.countByStatus(UserStatus.ACTIVE));
        model.addAttribute("productCount", productRepository.count());
        model.addAttribute("hiddenProductCount", productRepository.countByStatus(ProductStatus.HIDDEN));
        model.addAttribute("pendingReportCount", reportRepository.countByStatus(ReportStatus.RECEIVED));
        model.addAttribute("logs", auditLogRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, 20)));
        return "admin/dashboard";
    }
}
