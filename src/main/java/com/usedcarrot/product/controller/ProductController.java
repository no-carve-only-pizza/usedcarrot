package com.usedcarrot.product.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.crypto.EthFormatter;
import com.usedcarrot.crypto.EthereumProperties;
import com.usedcarrot.favorite.service.FavoriteService;
import com.usedcarrot.product.domain.Product;
import com.usedcarrot.product.domain.ProductCategories;
import com.usedcarrot.product.domain.ProductStatus;
import com.usedcarrot.product.dto.ProductCreateRequest;
import com.usedcarrot.product.dto.ProductSearchCondition;
import com.usedcarrot.product.dto.ProductUpdateRequest;
import com.usedcarrot.product.service.ProductService;
import com.usedcarrot.user.service.UserService;
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
    private final FavoriteService favoriteService;
    private final UserService userService;
    private final EthereumProperties ethereumProperties;

    public ProductController(ProductService productService, FavoriteService favoriteService,
                             UserService userService, EthereumProperties ethereumProperties) {
        this.productService = productService;
        this.favoriteService = favoriteService;
        this.userService = userService;
        this.ethereumProperties = ethereumProperties;
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
                       @RequestParam(defaultValue = "12") int size, Model model) {
        model.addAttribute("page", productService.search(condition, page, size));
        model.addAttribute("statuses", ProductStatus.publicFilterValues());
        model.addAttribute("categories", ProductCategories.ALL);
        model.addAttribute("mine", false);
        return "products/list";
    }

    @GetMapping("/products/new")
    public String newForm(Model model) {
        model.addAttribute("productForm", new ProductCreateRequest());
        model.addAttribute("categories", ProductCategories.ALL);
        return "products/form";
    }

    @PostMapping("/products")
    public String create(@AuthenticationPrincipal CurrentUser currentUser,
                         @Valid @ModelAttribute("productForm") ProductCreateRequest request, BindingResult bindingResult,
                         @RequestParam(required = false) List<MultipartFile> images,
                         HttpServletRequest servletRequest, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formError", "입력한 상품 정보를 다시 확인해주세요.");
            model.addAttribute("categories", ProductCategories.ALL);
            return "products/form";
        }
        try {
            Product product = productService.create(currentUser, request, images, servletRequest);
            return "redirect:/products/" + product.getId();
        } catch (AppException e) {
            model.addAttribute("formError", e.getMessage());
            model.addAttribute("categories", ProductCategories.ALL);
            return "products/form";
        }
    }

    @GetMapping("/products/{id}")
    public String detail(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser, Model model) {
        Product product = productService.detail(id);
        boolean owner = currentUser != null && product.isSeller(currentUser.getId());
        boolean admin = currentUser != null && currentUser.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean sellerHasWallet = product.getPayToAddress() != null
            && !product.getPayToAddress().isBlank();
        boolean buyerHasWallet = false;
        String buyerWallet = null;
        if (currentUser != null) {
            var me = userService.current(currentUser);
            buyerHasWallet = me.getWalletAddress() != null && !me.getWalletAddress().isBlank();
            buyerWallet = me.getWalletAddress();
        }
        model.addAttribute("product", product);
        model.addAttribute("owner", owner);
        model.addAttribute("admin", admin);
        model.addAttribute("favorited", currentUser != null && favoriteService.isFavorited(currentUser, id));
        model.addAttribute("canFavorite", currentUser != null && !owner);
        model.addAttribute("canChat", currentUser != null && !owner && product.isPurchasable());
        model.addAttribute("canPurchase", currentUser != null && !owner && product.isPurchasable()
            && sellerHasWallet && buyerHasWallet);
        model.addAttribute("purchaseBlockedReason", purchaseBlockedReason(currentUser, owner, product, sellerHasWallet, buyerHasWallet));
        model.addAttribute("canReportProduct", currentUser != null && !owner);
        model.addAttribute("canReportSeller", currentUser != null && !owner);
        model.addAttribute("canEdit", owner || admin);
        model.addAttribute("canDelete", (owner && product.getStatus().isSellerEditable()) || admin);
        model.addAttribute("chain", ethereumProperties);
        model.addAttribute("sellerWallet", product.getPayToAddress());
        model.addAttribute("buyerWallet", buyerWallet);
        model.addAttribute("priceWei", String.valueOf(product.getPrice()));
        model.addAttribute("paymentData", com.usedcarrot.crypto.PaymentMemo.calldataHex(product.getId()));
        return "products/detail";
    }

    private String purchaseBlockedReason(CurrentUser currentUser, boolean owner, Product product,
                                         boolean sellerHasWallet, boolean buyerHasWallet) {
        if (currentUser == null || owner || !product.isPurchasable()) {
            return null;
        }
        if (!sellerHasWallet) {
            return "상품에 결제 주소가 없습니다.";
        }
        if (!buyerHasWallet) {
            return "구매하려면 마이페이지에서 MetaMask 지갑을 먼저 연결하세요.";
        }
        return null;
    }

    @GetMapping("/products/{id}/edit")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser, Model model) {
        Product product = productService.findForEdit(id, currentUser);
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setTitle(product.getTitle());
        request.setPriceEth(EthFormatter.fromWei(product.getPrice()));
        request.setCategory(product.getCategory());
        request.setRegion(product.getRegion());
        request.setDescription(product.getDescription());
        request.setStatus(product.getStatus().isSellerEditable() ? product.getStatus() : ProductStatus.ON_SALE);
        model.addAttribute("product", product);
        model.addAttribute("productForm", request);
        model.addAttribute("statuses", sellerStatusesFor(currentUser));
        model.addAttribute("categories", ProductCategories.ALL);
        return "products/form";
    }

    @PostMapping("/products/{id}/edit")
    public String update(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser,
                         @Valid @ModelAttribute("productForm") ProductUpdateRequest request, BindingResult bindingResult,
                         @RequestParam(required = false) List<MultipartFile> images,
                         HttpServletRequest servletRequest, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formError", "입력한 상품 정보를 다시 확인해주세요.");
            model.addAttribute("product", productService.findForEdit(id, currentUser));
            model.addAttribute("statuses", sellerStatusesFor(currentUser));
            model.addAttribute("categories", ProductCategories.ALL);
            return "products/form";
        }
        try {
            productService.update(id, currentUser, request, images, servletRequest);
            return "redirect:/products/" + id;
        } catch (AppException e) {
            model.addAttribute("formError", e.getMessage());
            model.addAttribute("product", productService.findForEdit(id, currentUser));
            model.addAttribute("statuses", sellerStatusesFor(currentUser));
            model.addAttribute("categories", ProductCategories.ALL);
            return "products/form";
        }
    }

    @PostMapping("/products/{id}/delete")
    public String delete(@PathVariable Long id, @AuthenticationPrincipal CurrentUser currentUser,
                         HttpServletRequest servletRequest, RedirectAttributes redirectAttributes) {
        productService.delete(id, currentUser, servletRequest);
        redirectAttributes.addFlashAttribute("message", "상품이 삭제되었습니다.");
        return "redirect:/products/me";
    }

    @GetMapping("/products/me")
    public String mine(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        model.addAttribute("products", productService.myProducts(currentUser));
        model.addAttribute("mine", true);
        model.addAttribute("condition", new ProductSearchCondition());
        model.addAttribute("categories", ProductCategories.ALL);
        model.addAttribute("statuses", ProductStatus.publicFilterValues());
        return "products/list";
    }

    private ProductStatus[] sellerStatusesFor(CurrentUser currentUser) {
        boolean admin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return admin ? ProductStatus.values() : ProductStatus.sellerEditableValues();
    }
}
