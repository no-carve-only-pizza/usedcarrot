package com.usedcarrot.report.service;

import com.usedcarrot.audit.domain.AuditEventType;
import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.common.AuditLogger;
import com.usedcarrot.common.ErrorCode;
import com.usedcarrot.product.domain.Product;
import com.usedcarrot.product.service.ProductService;
import com.usedcarrot.report.domain.Report;
import com.usedcarrot.report.domain.ReportStatus;
import com.usedcarrot.report.domain.ReportTargetType;
import com.usedcarrot.report.dto.ReportCreateRequest;
import com.usedcarrot.report.repository.ReportRepository;
import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserStatus;
import com.usedcarrot.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final ReportRepository reportRepository;
    private final UserService userService;
    private final ProductService productService;
    private final AuditLogger auditLogger;

    public ReportService(ReportRepository reportRepository, UserService userService, ProductService productService,
                         AuditLogger auditLogger) {
        this.reportRepository = reportRepository;
        this.userService = userService;
        this.productService = productService;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public void create(CurrentUser currentUser, ReportCreateRequest request, HttpServletRequest servletRequest) {
        User reporter = userService.findById(currentUser.getId());
        if (reporter.getStatus() == UserStatus.SUSPENDED || reporter.getStatus() == UserStatus.DELETED) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "정지된 사용자는 신고할 수 없습니다.");
        }
        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndCreatedAtAfter(reporter.getId(), request.getTargetType(),
            request.getTargetId(), LocalDateTime.now().minusHours(24))) {
            throw new AppException(ErrorCode.DUPLICATE_RESOURCE, "24시간 내 같은 대상을 다시 신고할 수 없습니다.");
        }
        validateTarget(reporter, request);
        Report report = reportRepository.save(new Report(reporter, request.getTargetType(), request.getTargetId(), request.getReason(), request.getDetail()));
        auditLogger.log(reporter.getId(), AuditEventType.REPORT_CREATED, "SUCCESS", "reportId=" + report.getId(), servletRequest);
    }

    private void validateTarget(User reporter, ReportCreateRequest request) {
        if (request.getTargetType() == ReportTargetType.PRODUCT) {
            Product product = productService.find(request.getTargetId());
            if (product.isSeller(reporter.getId())) {
                throw new AppException(ErrorCode.BAD_REQUEST, "본인 상품은 신고할 수 없습니다.");
            }
            return;
        }
        User target = userService.findById(request.getTargetId());
        if (target.getId().equals(reporter.getId())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "자기 자신은 신고할 수 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public List<Report> all() {
        return reportRepository.findByOrderByCreatedAtDesc();
    }

    @Transactional
    public void process(Long reportId, ReportStatus status, String memo, CurrentUser admin, HttpServletRequest servletRequest) {
        if (status != ReportStatus.RESOLVED && status != ReportStatus.REJECTED) {
            throw new AppException(ErrorCode.BAD_REQUEST, "신고는 승인 또는 반려로 처리해야 합니다.");
        }
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "신고를 찾을 수 없습니다."));
        if (report.isProcessed()) {
            throw new AppException(ErrorCode.INVALID_STATE, "이미 처리된 신고입니다.");
        }
        report.process(status, memo, userService.findById(admin.getId()));
        if (status == ReportStatus.RESOLVED) {
            applyApprovedReport(report);
        }
        auditLogger.log(admin.getId(), AuditEventType.REPORT_PROCESSED, "SUCCESS", "reportId=" + reportId + ",status=" + status, servletRequest);
    }

    private void applyApprovedReport(Report report) {
        if (report.getTargetType() == ReportTargetType.PRODUCT) {
            productService.find(report.getTargetId()).increaseReportCount();
            return;
        }
        User target = userService.findById(report.getTargetId());
        long count = reportRepository.countByTargetTypeAndTargetIdAndStatus(
            ReportTargetType.USER, target.getId(), ReportStatus.RESOLVED);
        if (count >= 10) {
            target.changeStatus(UserStatus.SUSPENDED);
        } else if (count >= 5) {
            target.changeStatus(UserStatus.LIMITED);
        }
    }
}
