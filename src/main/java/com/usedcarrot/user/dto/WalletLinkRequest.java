package com.usedcarrot.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class WalletLinkRequest {
    @NotBlank
    @Pattern(regexp = "(?i)^0x[0-9a-f]{40}$")
    private String address;

    @NotBlank
    @Size(min = 10, max = 500)
    private String message;

    @NotBlank
    @Pattern(regexp = "(?i)^0x[0-9a-f]+$")
    private String signature;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
