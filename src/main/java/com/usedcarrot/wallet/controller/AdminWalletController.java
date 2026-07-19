package com.usedcarrot.wallet.controller;

import com.usedcarrot.crypto.EthereumProperties;
import com.usedcarrot.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminWalletController {
    private final WalletTransactionRepository walletTransactionRepository;
    private final EthereumProperties ethereumProperties;

    public AdminWalletController(WalletTransactionRepository walletTransactionRepository,
                                 EthereumProperties ethereumProperties) {
        this.walletTransactionRepository = walletTransactionRepository;
        this.ethereumProperties = ethereumProperties;
    }

    @GetMapping("/admin/wallet-transactions")
    public String transactions(Model model) {
        model.addAttribute("transactions", walletTransactionRepository.findAll());
        model.addAttribute("chain", ethereumProperties);
        return "admin/wallet-transactions";
    }
}
