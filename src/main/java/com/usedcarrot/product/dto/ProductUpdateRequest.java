package com.usedcarrot.product.dto;

import com.usedcarrot.product.domain.ProductStatus;
import jakarta.validation.constraints.NotNull;

public class ProductUpdateRequest extends ProductCreateRequest {
    @NotNull
    private ProductStatus status = ProductStatus.ON_SALE;

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }
}
