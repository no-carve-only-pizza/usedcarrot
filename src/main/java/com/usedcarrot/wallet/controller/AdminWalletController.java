package com.usedcarrot.wallet.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.wallet.repository.WalletTransactionRepository;
import com.usedcarrot.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminWalletController {
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletService walletService;

    public AdminWalletController(WalletTransactionRepository walletTransactionRepository, WalletService walletService) {
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletService = walletService;
    }

    @GetMapping("/admin/wallet-transactions")
    public String transactions(Model model) {
        model.addAttribute("transactions", walletTransactionRepository.findAll());
        return "wallets/transactions";
    }

    @PostMapping("/admin/users/{userId}/wallet/grant")
    public String grant(@PathVariable Long userId, @RequestParam long amount,
                        @AuthenticationPrincipal CurrentUser admin, HttpServletRequest request) {
        walletService.adminGrant(userId, amount, admin, request);
        return "redirect:/admin/users";
    }
}
