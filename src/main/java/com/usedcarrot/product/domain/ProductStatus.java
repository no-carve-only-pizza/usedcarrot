package com.usedcarrot.product.domain;

public enum ProductStatus {
    ON_SALE,
    RESERVED,
    SOLD,
    HIDDEN,
    DELETED;

    public boolean isSellerEditable() {
        return this == ON_SALE || this == RESERVED;
    }

    public boolean isPublicFilterable() {
        return this == ON_SALE || this == RESERVED || this == SOLD;
    }

    public static ProductStatus[] sellerEditableValues() {
        return new ProductStatus[]{ON_SALE, RESERVED};
    }

    public static ProductStatus[] publicFilterValues() {
        return new ProductStatus[]{ON_SALE, RESERVED, SOLD};
    }
}

