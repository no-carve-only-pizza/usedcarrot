package com.usedcarrot.report.repository;

import com.usedcarrot.report.domain.Report;
import com.usedcarrot.report.domain.ReportStatus;
import com.usedcarrot.report.domain.ReportTargetType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByReporterIdAndTargetTypeAndTargetIdAndCreatedAtAfter(Long reporterId, ReportTargetType targetType, Long targetId, LocalDateTime after);
    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);
    long countByTargetTypeAndTargetIdAndStatus(ReportTargetType targetType, Long targetId, ReportStatus status);
    long countByStatus(ReportStatus status);
    List<Report> findByOrderByCreatedAtDesc();
}
