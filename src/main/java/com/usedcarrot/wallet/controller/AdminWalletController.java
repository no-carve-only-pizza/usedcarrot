package com.usedcarrot.wallet.controller;

import com.usedcarrot.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminWalletController {
    private final WalletTransactionRepository walletTransactionRepository;

    public AdminWalletController(WalletTransactionRepository walletTransactionRepository) {
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @GetMapping("/admin/wallet-transactions")
    public String transactions(Model model) {
        model.addAttribute("transactions", walletTransactionRepository.findAll());
        return "wallets/transactions";
    }
}
