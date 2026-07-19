package com.usedcarrot.chat.domain;

import com.usedcarrot.common.BaseTimeEntity;
import com.usedcarrot.product.domain.Product;
import com.usedcarrot.user.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "chat_rooms", uniqueConstraints = @UniqueConstraint(name = "uq_chat_room", columnNames = {"product_id", "buyer_id", "seller_id"}))
public class ChatRoom extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id")
    private User seller;

    protected ChatRoom() {
    }

    public ChatRoom(Product product, User buyer, User seller) {
        this.product = product;
        this.buyer = buyer;
        this.seller = seller;
    }

    public boolean isParticipant(Long userId) {
        return buyer.getId().equals(userId) || seller.getId().equals(userId);
    }

    public User opponent(Long userId) {
        return buyer.getId().equals(userId) ? seller : buyer;
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
}
