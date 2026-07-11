package com.usedcarrot.product.domain;

import com.usedcarrot.common.BaseTimeEntity;
import com.usedcarrot.user.domain.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id")
    private User seller;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 50)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ON_SALE;

    @Column(nullable = false)
    private long viewCount;

    @Column(nullable = false)
    private int reportCount;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    protected Product() {
    }

    public Product(User seller, String title, String description, long price, String category, String region) {
        this.seller = seller;
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.region = region;
    }

    public boolean isSeller(Long userId) {
        return seller.getId().equals(userId);
    }

    public boolean isVisible() {
        return status != ProductStatus.HIDDEN && status != ProductStatus.DELETED;
    }

    public boolean isPurchasable() {
        return status == ProductStatus.ON_SALE || status == ProductStatus.RESERVED;
    }

    public void update(String title, String description, long price, String category, String region, ProductStatus status) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.category = category;
        this.region = region;
        this.status = status;
    }

    public void addImage(ProductImage image) {
        images.add(image);
        image.attach(this);
    }

    public void increaseViewCount() {
        viewCount++;
    }

    public void increaseReportCount() {
        reportCount++;
        if (reportCount >= 3 && status != ProductStatus.DELETED) {
            status = ProductStatus.HIDDEN;
        }
    }

    public void changeStatus(ProductStatus status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public User getSeller() {
        return seller;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public long getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public String getRegion() {
        return region;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public long getViewCount() {
        return viewCount;
    }

    public int getReportCount() {
        return reportCount;
    }

    public List<ProductImage> getImages() {
        return images;
    }
}
