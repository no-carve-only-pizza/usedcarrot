package com.usedcarrot.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductCreateRequest {
    @NotBlank
    @Size(min = 2, max = 80)
    private String title;

    @NotNull
    @Min(0)
    @Max(100000000)
    private Long price;

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

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
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
