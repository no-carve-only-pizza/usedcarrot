package com.usedcarrot.user.domain;

import com.usedcarrot.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
    @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname")
})
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false, length = 50)
    private String region;

    @Column(length = 200)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.ROLE_USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    private int loginFailCount;

    private LocalDateTime lockedUntil;

    protected User() {
    }

    public User(String email, String passwordHash, String nickname, String region, UserRole role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.region = region;
        this.role = role;
    }

    public boolean isUsable() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isLoginAllowed() {
        return status != UserStatus.SUSPENDED && status != UserStatus.DELETED;
    }

    public void updateProfile(String nickname, String region, String bio) {
        this.nickname = nickname;
        this.region = region;
        this.bio = bio;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void changeStatus(UserStatus status) {
        this.status = status;
    }

    public void loginSucceeded() {
        this.loginFailCount = 0;
        this.lockedUntil = null;
    }

    public void loginFailed() {
        this.loginFailCount++;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNickname() {
        return nickname;
    }

    public String getRegion() {
        return region;
    }

    public String getBio() {
        return bio;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public int getLoginFailCount() {
        return loginFailCount;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
}
