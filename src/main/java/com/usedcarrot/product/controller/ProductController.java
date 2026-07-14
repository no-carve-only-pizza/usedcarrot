package com.usedcarrot.product.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.product.domain.Product;
import com.usedcarrot.product.domain.ProductStatus;
import com.usedcarrot.product.dto.ProductCreateRequest;
import com.usedcarrot.product.dto.ProductSearchCondition;
import com.usedcarrot.product.dto.ProductUpdateRequest;
import com.usedcarrot.product.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("products", productService.latest());
        model.addAttribute("condition", new ProductSearchCondition());
        return "index";
    }

    @GetMapping("/products")
    public String list(@Valid @ModelAttribute("condition") ProductSearchCondition condition,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size, Model model) {
        model.addAttribute("page", productService.search(condition, page, size));
        model.addAttribute("statuses", ProductStatus.values());
        return "products/list";
    }

    @GetMapping("/products/new")
    public String newForm(Model model) {
        model.addAttribute("productCreateRequest", new ProductCreateRequest());
        return "products/form";
    }

    @PostMapping("/products")
    public String create(@AuthenticationPrincipal CurrentUser currentUser,
                         @Valid @ModelAttribute ProductCreateRequest request, BindingResult bindingResult,
                         @RequestParam(required = false) List<MultipartFile> images,
                         HttpServletRequest servletRequest, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formError", "입력한 상품 정보를 다시 확인해주세요.");
            return "products/form";
        }
        Product product = productService.create(currentUser, request, images, servletRequest);
        return "redirect:/products/" + product.getId();
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.detail(id));
        model.addAttribute("idempotencyKey", UUID.randomUUID().toString());
        return "products/detail";
    }

    @GetMapping("/products/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser, Model model) {
        Product product = productService.findForEdit(id, currentUser);
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setTitle(product.getTitle());
        request.setPrice(product.getPrice());
        request.setCategory(product.getCategory());
        request.setRegion(product.getRegion());
        request.setDescription(product.getDescription());
        request.setStatus(product.getStatus());
        model.addAttribute("product", product);
        model.addAttribute("productUpdateRequest", request);
        model.addAttribute("statuses", ProductStatus.values());
        return "products/form";
    }

    @PostMapping("/products/{id}/edit")
    public String update(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser,
                         @Valid @ModelAttribute ProductUpdateRequest request, BindingResult bindingResult,
                         HttpServletRequest servletRequest, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formError", "입력한 상품 정보를 다시 확인해주세요.");
            model.addAttribute("product", productService.findForEdit(id, currentUser));
            model.addAttribute("statuses", ProductStatus.values());
            return "products/form";
        }
        productService.update(id, currentUser, request, servletRequest);
        return "redirect:/products/" + id;
    }

    @PostMapping("/products/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser,
                         HttpServletRequest servletRequest, RedirectAttributes redirectAttributes) {
        productService.delete(id, currentUser, servletRequest);
        redirectAttributes.addFlashAttribute("message", "상품이 삭제되었습니다.");
        return "redirect:/products";
    }

    @GetMapping("/products/me")
    public String mine(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        model.addAttribute("products", productService.myProducts(currentUser));
        return "products/list";
    }
}
