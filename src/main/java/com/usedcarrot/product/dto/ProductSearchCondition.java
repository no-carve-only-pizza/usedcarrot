package com.usedcarrot.product.dto;

import com.usedcarrot.product.domain.ProductStatus;
import jakarta.validation.constraints.Size;

public class ProductSearchCondition {
    @Size(max = 50)
    private String keyword;
    private String category;
    private ProductStatus status;
    private String region;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
