package com.usedcarrot.report.dto;

import com.usedcarrot.report.domain.ReportReason;
import com.usedcarrot.report.domain.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReportCreateRequest {
    @NotNull
    private ReportTargetType targetType;

    @NotNull
    private Long targetId;

    @NotNull
    private ReportReason reason;

    @Size(max = 1000)
    private String detail;

    public ReportTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(ReportTargetType targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public ReportReason getReason() {
        return reason;
    }

    public void setReason(ReportReason reason) {
        this.reason = reason;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
