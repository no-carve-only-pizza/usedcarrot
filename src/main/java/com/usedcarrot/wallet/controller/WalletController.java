package com.usedcarrot.wallet.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.wallet.dto.WalletTransferRequest;
import com.usedcarrot.wallet.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WalletController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/wallet")
    public String detail(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        model.addAttribute("wallet", walletService.myWallet(currentUser));
        model.addAttribute("transactions", walletService.myTransactions(currentUser));
        return "wallets/detail";
    }

    @GetMapping("/wallet/transactions")
    public String transactions(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        model.addAttribute("transactions", walletService.myTransactions(currentUser));
        return "wallets/transactions";
    }

    @PostMapping("/wallet/transfers")
    public String transfer(@AuthenticationPrincipal CurrentUser currentUser,
                           @Valid @ModelAttribute WalletTransferRequest request, BindingResult bindingResult,
                           HttpServletRequest servletRequest, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "redirect:/products/" + request.getProductId();
        }
        walletService.purchase(currentUser, request.getProductId(), request.getIdempotencyKey(), servletRequest);
        redirectAttributes.addFlashAttribute("message", "CarrotCoin 구매가 완료되었습니다.");
        return "redirect:/wallet";
    }
}
