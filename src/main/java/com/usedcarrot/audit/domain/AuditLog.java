package com.usedcarrot.audit.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(length = 50)
    private String ipAddress;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(length = 1000)
    private String detail;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected AuditLog() {
    }

    public AuditLog(Long userId, AuditEventType eventType, String ipAddress, String result, String detail) {
        this.userId = userId;
        this.eventType = eventType;
        this.ipAddress = ipAddress;
        this.result = result;
        this.detail = detail;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getResult() {
        return result;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
