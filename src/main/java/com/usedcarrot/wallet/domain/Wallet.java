package com.usedcarrot.wallet.domain;

import com.usedcarrot.common.BaseTimeEntity;
import com.usedcarrot.common.AppException;
import com.usedcarrot.common.ErrorCode;
import com.usedcarrot.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "wallets", uniqueConstraints = @UniqueConstraint(name = "uk_wallets_user", columnNames = "user_id"))
public class Wallet extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private long balance;

    protected Wallet() {
    }

    public Wallet(User user, long balance) {
        this.user = user;
        this.balance = balance;
    }

    public void withdraw(long amount) {
        if (amount <= 0 || balance < amount) {
            throw new AppException(ErrorCode.INVALID_STATE, "CarrotCoin 잔액이 부족합니다.");
        }
        balance -= amount;
    }

    public void deposit(long amount) {
        if (amount <= 0) {
            throw new AppException(ErrorCode.BAD_REQUEST, "거래 금액이 올바르지 않습니다.");
        }
        balance += amount;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public long getBalance() {
        return balance;
    }
}
