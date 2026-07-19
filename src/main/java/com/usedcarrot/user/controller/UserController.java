package com.usedcarrot.user.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.crypto.EthereumProperties;
import com.usedcarrot.user.dto.AccountWithdrawRequest;
import com.usedcarrot.user.dto.PasswordChangeRequest;
import com.usedcarrot.user.dto.ProfileUpdateRequest;
import com.usedcarrot.user.dto.WalletLinkRequest;
import com.usedcarrot.user.service.UserService;
import com.usedcarrot.user.service.WalletLinkNonceStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {
    private final UserService userService;
    private final EthereumProperties ethereumProperties;

    public UserController(UserService userService, EthereumProperties ethereumProperties) {
        this.userService = userService;
        this.ethereumProperties = ethereumProperties;
    }

    @GetMapping("/users/me")
    public String me(@AuthenticationPrincipal CurrentUser currentUser, HttpServletRequest request, Model model) {
        populateMe(currentUser, request, model);
        return "users/me";
    }

    @PostMapping("/users/me/profile")
    public String updateProfile(@AuthenticationPrincipal CurrentUser currentUser,
                                @Valid @ModelAttribute ProfileUpdateRequest request,
                                BindingResult bindingResult, HttpServletRequest servletRequest,
                                Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateMe(currentUser, servletRequest, model);
            model.addAttribute("profileUpdateRequest", request);
            return "users/me";
        }
        try {
            userService.updateProfile(currentUser, request);
        } catch (AppException e) {
            populateMe(currentUser, servletRequest, model);
            model.addAttribute("profileUpdateRequest", request);
            model.addAttribute("error", e.getMessage());
            return "users/me";
        }
        redirectAttributes.addFlashAttribute("message", "프로필이 수정되었습니다.");
        return "redirect:/users/me";
    }

    @PostMapping("/users/me/wallet")
    public String linkWallet(@AuthenticationPrincipal CurrentUser currentUser,
                             @Valid @ModelAttribute WalletLinkRequest request,
                             BindingResult bindingResult, HttpServletRequest servletRequest,
                             Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateMe(currentUser, servletRequest, model);
            model.addAttribute("error", "지갑 연결 요청이 올바르지 않습니다.");
            return "users/me";
        }
        try {
            userService.linkWallet(currentUser, request, servletRequest);
        } catch (AppException e) {
            populateMe(currentUser, servletRequest, model);
            model.addAttribute("error", e.getMessage());
            return "users/me";
        }
        redirectAttributes.addFlashAttribute("message", "MetaMask 지갑이 연결되었습니다.");
        return "redirect:/users/me";
    }

    @PostMapping("/users/me/password")
    public String changePassword(@AuthenticationPrincipal CurrentUser currentUser,
                                 @Valid @ModelAttribute PasswordChangeRequest request,
                                 BindingResult bindingResult, HttpServletRequest servletRequest,
                                 Model model, RedirectAttributes redirectAttributes) {
        if (!request.passwordMatches()) {
            bindingResult.rejectValue("newPasswordConfirm", "mismatch", "새 비밀번호가 일치하지 않습니다.");
        }
        if (bindingResult.hasErrors()) {
            populateMe(currentUser, servletRequest, model);
            model.addAttribute("passwordChangeRequest", request);
            return "users/me";
        }
        try {
            userService.changePassword(currentUser, request, servletRequest);
        } catch (AppException e) {
            populateMe(currentUser, servletRequest, model);
            model.addAttribute("passwordChangeRequest", request);
            model.addAttribute("error", e.getMessage());
            return "users/me";
        }
        redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
        return "redirect:/users/me";
    }

    @PostMapping("/users/me/withdraw")
    public String withdraw(@AuthenticationPrincipal CurrentUser currentUser,
                           @Valid @ModelAttribute AccountWithdrawRequest request,
                           BindingResult bindingResult, HttpServletRequest servletRequest,
                           HttpServletResponse response, Model model, RedirectAttributes redirectAttributes) {
        if (!"탈퇴합니다".equals(request.getConfirmText())) {
            bindingResult.rejectValue("confirmText", "mismatch", "확인 문구를 정확히 입력해주세요.");
        }
        if (bindingResult.hasErrors()) {
            populateMe(currentUser, servletRequest, model);
            model.addAttribute("accountWithdrawRequest", request);
            return "users/me";
        }
        try {
            userService.withdraw(currentUser, request.getCurrentPassword(), servletRequest);
        } catch (AppException e) {
            populateMe(currentUser, servletRequest, model);
            model.addAttribute("accountWithdrawRequest", request);
            model.addAttribute("error", e.getMessage());
            return "users/me";
        }
        new SecurityContextLogoutHandler().logout(servletRequest, response, SecurityContextHolder.getContext().getAuthentication());
        redirectAttributes.addFlashAttribute("message", "계정이 탈퇴 처리되었습니다.");
        return "redirect:/login?withdrawn";
    }

    private void populateMe(CurrentUser currentUser, HttpServletRequest request, Model model) {
        var user = userService.current(currentUser);
        if (!model.containsAttribute("profileUpdateRequest")) {
            ProfileUpdateRequest profile = new ProfileUpdateRequest();
            profile.setNickname(user.getNickname());
            profile.setRegion(user.getRegion());
            profile.setBio(user.getBio());
            model.addAttribute("profileUpdateRequest", profile);
        }
        if (!model.containsAttribute("passwordChangeRequest")) {
            model.addAttribute("passwordChangeRequest", new PasswordChangeRequest());
        }
        if (!model.containsAttribute("accountWithdrawRequest")) {
            model.addAttribute("accountWithdrawRequest", new AccountWithdrawRequest());
        }
        model.addAttribute("user", user);
        model.addAttribute("chain", ethereumProperties);
        String nonce = WalletLinkNonceStore.issue(request.getSession(true));
        model.addAttribute("walletLinkMessage",
            "UsedCarrot wallet link|userId=" + user.getId() + "|nonce=" + nonce);
    }
}
