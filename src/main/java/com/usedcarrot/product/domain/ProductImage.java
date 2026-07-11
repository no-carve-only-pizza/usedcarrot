package com.usedcarrot.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_images")
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected ProductImage() {
    }

    public ProductImage(String originalFileName, String storedFileName, String mimeType, long fileSize, String path) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.mimeType = mimeType;
        this.fileSize = fileSize;
        this.path = path;
    }

    void attach(Product product) {
        this.product = product;
    }

    public Product getProduct() {
        return product;
    }

    public String getUrl() {
        return "/uploads/" + storedFileName;
    }

    public Long getId() {
        return id;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getPath() {
        return path;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
