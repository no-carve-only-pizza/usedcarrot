package com.usedcarrot.product.domain;

import java.util.List;

public final class ProductCategories {
    public static final List<String> ALL = List.of(
        "디지털/가전",
        "가구/인테리어",
        "생활/주방",
        "유아동",
        "의류",
        "도서/취미",
        "스포츠/레저",
        "기타"
    );

    private ProductCategories() {
    }

    public static boolean isAllowed(String category) {
        return category != null && ALL.contains(category);
    }
}
