package com.usedcarrot.wallet.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.crypto.EthereumProperties;
import com.usedcarrot.user.service.UserService;
import com.usedcarrot.wallet.dto.OnChainPurchaseRequest;
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
    private final UserService userService;
    private final EthereumProperties ethereumProperties;

    public WalletController(WalletService walletService, UserService userService, EthereumProperties ethereumProperties) {
        this.walletService = walletService;
        this.userService = userService;
        this.ethereumProperties = ethereumProperties;
    }

    @GetMapping("/wallet")
    public String detail(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        model.addAttribute("user", userService.current(currentUser));
        model.addAttribute("transactions", walletService.myTransactions(currentUser));
        model.addAttribute("chain", ethereumProperties);
        return "wallets/detail";
    }

    @GetMapping("/wallet/transactions")
    public String transactions(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        model.addAttribute("transactions", walletService.myTransactions(currentUser));
        model.addAttribute("chain", ethereumProperties);
        return "wallets/transactions";
    }

    @PostMapping("/wallet/onchain-purchases")
    public String confirmPurchase(@AuthenticationPrincipal CurrentUser currentUser,
                                  @Valid @ModelAttribute OnChainPurchaseRequest request, BindingResult bindingResult,
                                  HttpServletRequest servletRequest, RedirectAttributes redirectAttributes) {
        Long productId = request.getProductId();
        if (bindingResult.hasErrors() || productId == null) {
            redirectAttributes.addFlashAttribute("error", "온체인 결제 확정 요청이 올바르지 않습니다.");
            return productId == null ? "redirect:/products" : "redirect:/products/" + productId;
        }
        try {
            walletService.confirmOnChainPurchase(currentUser, productId, request.getTxHash(), servletRequest);
            redirectAttributes.addFlashAttribute("message", "온체인 결제가 확인되어 거래가 완료되었습니다.");
            return "redirect:/wallet";
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/products/" + productId;
        }
    }
}
