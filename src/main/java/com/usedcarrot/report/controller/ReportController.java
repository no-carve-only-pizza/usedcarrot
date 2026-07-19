package com.usedcarrot.report.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.report.domain.ReportReason;
import com.usedcarrot.report.domain.ReportTargetType;
import com.usedcarrot.report.dto.ReportCreateRequest;
import com.usedcarrot.report.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports/new")
    public String newForm(@RequestParam ReportTargetType targetType, @RequestParam Long targetId, Model model) {
        ReportCreateRequest request = new ReportCreateRequest();
        request.setTargetType(targetType);
        request.setTargetId(targetId);
        model.addAttribute("reportCreateRequest", request);
        model.addAttribute("reasons", ReportReason.values());
        return "reports/form";
    }

    @PostMapping("/reports")
    public String create(@AuthenticationPrincipal CurrentUser currentUser,
                         @Valid @ModelAttribute ReportCreateRequest request, BindingResult bindingResult,
                         HttpServletRequest servletRequest, RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("reasons", ReportReason.values());
            return "reports/form";
        }
        reportService.create(currentUser, request, servletRequest);
        redirectAttributes.addFlashAttribute("message", "신고가 접수되었습니다.");
        return "redirect:/";
    }
}
