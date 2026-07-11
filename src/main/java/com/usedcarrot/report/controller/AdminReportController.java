package com.usedcarrot.report.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.report.domain.ReportStatus;
import com.usedcarrot.report.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminReportController {
    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/admin/reports")
    public String reports(Model model) {
        model.addAttribute("reports", reportService.all());
        model.addAttribute("statuses", ReportStatus.values());
        return "admin/reports";
    }

    @PostMapping("/admin/reports/{id}/process")
    public String process(@PathVariable Long id, @RequestParam ReportStatus status, @RequestParam(required = false) String memo,
                          @AuthenticationPrincipal CurrentUser admin, HttpServletRequest request) {
        reportService.process(id, status, memo, admin, request);
        return "redirect:/admin/reports";
    }
}
