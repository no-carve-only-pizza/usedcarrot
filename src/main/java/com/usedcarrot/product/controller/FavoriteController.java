package com.usedcarrot.product.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.favorite.service.FavoriteService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/products/{id}/favorite")
    public String toggle(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser,
                         RedirectAttributes redirectAttributes) {
        try {
            favoriteService.toggle(currentUser, id);
            redirectAttributes.addFlashAttribute("message", "관심 상품이 업데이트되었습니다.");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products/" + id;
    }

    @GetMapping("/favorites")
    public String mine(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        model.addAttribute("favorites", favoriteService.mine(currentUser));
        return "products/favorites";
    }
}
