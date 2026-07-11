package com.usedcarrot.user.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.user.domain.UserStatus;
import com.usedcarrot.user.repository.UserRepository;
import com.usedcarrot.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminUserController {
    private final UserRepository userRepository;
    private final UserService userService;

    public AdminUserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @GetMapping("/admin/users")
    public String users(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("users", keyword == null || keyword.isBlank()
            ? userRepository.findAll()
            : userRepository.findByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(keyword, keyword));
        model.addAttribute("statuses", UserStatus.values());
        return "admin/users";
    }

    @PostMapping("/admin/users/{id}/status")
    public String status(@PathVariable Long id, @RequestParam UserStatus status,
                         @AuthenticationPrincipal CurrentUser admin, HttpServletRequest request) {
        userService.changeStatus(id, status, admin, request);
        return "redirect:/admin/users";
    }
}
