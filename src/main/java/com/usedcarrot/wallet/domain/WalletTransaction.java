package com.usedcarrot.wallet.domain;

import com.usedcarrot.common.BaseTimeEntity;
import com.usedcarrot.product.domain.Product;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "wallet_transactions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_wallet_tx_hash", columnNames = "transaction_hash"),
    @UniqueConstraint(name = "uk_wallet_tx_idempotency", columnNames = "idempotency_key")
})
public class WalletTransaction extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WalletTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletTransactionStatus status;

    @Column(name = "transaction_hash", nullable = false, length = 100)
    private String transactionHash;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(length = 500)
    private String failureReason;

    protected WalletTransaction() {
    }

    public WalletTransaction(Product product, User buyer, User seller, Wallet fromWallet, Wallet toWallet, long amount,
                             WalletTransactionType type, WalletTransactionStatus status, String transactionHash,
                             String idempotencyKey, String failureReason) {
        this.product = product;
        this.buyer = buyer;
        this.seller = seller;
        this.fromWallet = fromWallet;
        this.toWallet = toWallet;
        this.amount = amount;
        this.type = type;
        this.status = status;
        this.transactionHash = transactionHash;
        this.idempotencyKey = idempotencyKey;
        this.failureReason = failureReason;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public User getBuyer() {
        return buyer;
    }

    public User getSeller() {
        return seller;
    }

    public long getAmount() {
        return amount;
    }

    public WalletTransactionType getType() {
        return type;
    }

    public WalletTransactionStatus getStatus() {
        return status;
    }

    public String getTransactionHash() {
        return transactionHash;
    }
}
