package com.usedcarrot.product.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.product.domain.ProductStatus;
import com.usedcarrot.product.repository.ProductRepository;
import com.usedcarrot.product.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminProductController {
    private final ProductRepository productRepository;
    private final ProductService productService;

    public AdminProductController(ProductRepository productRepository, ProductService productService) {
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @GetMapping("/admin/products")
    public String products(Model model) {
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("statuses", ProductStatus.values());
        return "admin/products";
    }

    @PostMapping("/admin/products/{id}/status")
    public String status(@PathVariable Long id, @RequestParam ProductStatus status,
                         @AuthenticationPrincipal CurrentUser admin, HttpServletRequest request) {
        productService.adminStatus(id, status, admin, request);
        return "redirect:/admin/products";
    }
}
