package com.usedcarrot.user.dto;

import jakarta.validation.constraints.NotBlank;

public class AccountWithdrawRequest {
    @NotBlank
    private String currentPassword;

    @NotBlank
    private String confirmText;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getConfirmText() {
        return confirmText;
    }

    public void setConfirmText(String confirmText) {
        this.confirmText = confirmText;
    }
}
