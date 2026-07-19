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

    @Column(name = "wallet_address", length = 42, unique = true)
    private String walletAddress;

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

    public void updateProfile(String nickname, String region, String bio) {
        this.nickname = nickname;
        this.region = region;
        this.bio = bio;
    }

    public void linkWallet(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public void clearWallet() {
        this.walletAddress = null;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void changeStatus(UserStatus status) {
        this.status = status;
    }

    public void withdraw(String anonymizedEmail, String anonymizedNickname) {
        this.email = anonymizedEmail;
        this.nickname = anonymizedNickname;
        this.region = "탈퇴";
        this.bio = null;
        this.walletAddress = null;
        this.passwordHash = "{withdrawn}";
        this.status = UserStatus.DELETED;
        this.loginFailCount = 0;
        this.lockedUntil = null;
    }

    public void loginSucceeded() {
        this.loginFailCount = 0;
        this.lockedUntil = null;
    }

    public void loginFailed() {
        this.loginFailCount++;
        if (this.loginFailCount >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(15);
        }
    }

    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isLoginAllowed() {
        return !isAccountLocked() && status != UserStatus.SUSPENDED && status != UserStatus.DELETED;
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

    public String getWalletAddress() {
        return walletAddress;
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
