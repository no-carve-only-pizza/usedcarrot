package com.usedcarrot.report.domain;

import com.usedcarrot.common.BaseTimeEntity;
import com.usedcarrot.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportReason reason;

    @Column(length = 1000)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.RECEIVED;

    @Column(length = 1000)
    private String adminMemo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private User handledBy;

    private LocalDateTime handledAt;

    protected Report() {
    }

    public Report(User reporter, ReportTargetType targetType, Long targetId, ReportReason reason, String detail) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.detail = detail;
    }

    public void process(ReportStatus status, String adminMemo, User handledBy) {
        this.status = status;
        this.adminMemo = adminMemo;
        this.handledBy = handledBy;
        this.handledAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public User getReporter() {
        return reporter;
    }

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public ReportReason getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public String getAdminMemo() {
        return adminMemo;
    }
}
