package com.usedcarrot.product.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class ProductCreateRequest {
    @NotBlank
    @Size(min = 2, max = 80)
    private String title;

    /** ETH 단위 가격. 서버에서 wei로 변환해 저장한다. */
    @NotNull
    @DecimalMin(value = "0.000001", inclusive = true)
    @DecimalMax(value = "5.0", inclusive = true)
    private BigDecimal priceEth;

    @NotBlank
    @Size(max = 50)
    private String category;

    @NotBlank
    @Size(min = 2, max = 50)
    private String region;

    @NotBlank
    @Size(min = 10, max = 2000)
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getPriceEth() {
        return priceEth;
    }

    public void setPriceEth(BigDecimal priceEth) {
        this.priceEth = priceEth;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
