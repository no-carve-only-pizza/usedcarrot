package com.usedcarrot.user.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.user.dto.PasswordChangeRequest;
import com.usedcarrot.user.dto.ProfileUpdateRequest;
import com.usedcarrot.user.service.UserService;
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
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/me")
    public String me(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        var user = userService.current(currentUser);
        ProfileUpdateRequest profile = new ProfileUpdateRequest();
        profile.setNickname(user.getNickname());
        profile.setRegion(user.getRegion());
        profile.setBio(user.getBio());
        model.addAttribute("user", user);
        model.addAttribute("profileUpdateRequest", profile);
        model.addAttribute("passwordChangeRequest", new PasswordChangeRequest());
        return "users/me";
    }

    @PostMapping("/users/me/profile")
    public String updateProfile(@AuthenticationPrincipal CurrentUser currentUser,
                                @Valid @ModelAttribute ProfileUpdateRequest request,
                                BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "users/me";
        }
        userService.updateProfile(currentUser, request);
        redirectAttributes.addFlashAttribute("message", "프로필이 수정되었습니다.");
        return "redirect:/users/me";
    }

    @PostMapping("/users/me/password")
    public String changePassword(@AuthenticationPrincipal CurrentUser currentUser,
                                 @Valid @ModelAttribute PasswordChangeRequest request,
                                 BindingResult bindingResult, HttpServletRequest servletRequest,
                                 RedirectAttributes redirectAttributes) {
        if (!request.passwordMatches()) {
            bindingResult.rejectValue("newPasswordConfirm", "mismatch", "새 비밀번호가 일치하지 않습니다.");
        }
        if (bindingResult.hasErrors()) {
            return "users/me";
        }
        userService.changePassword(currentUser, request, servletRequest);
        redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
        return "redirect:/users/me";
    }
}
