package com.usedcarrot.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OnChainPurchaseRequest {
    @NotNull
    private Long productId;

    @NotBlank
    @Pattern(regexp = "(?i)^0x[0-9a-f]{64}$")
    private String txHash;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }
}
