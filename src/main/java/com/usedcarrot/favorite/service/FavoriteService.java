package com.usedcarrot.favorite.service;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.common.AppException;
import com.usedcarrot.common.ErrorCode;
import com.usedcarrot.favorite.domain.Favorite;
import com.usedcarrot.favorite.repository.FavoriteRepository;
import com.usedcarrot.product.service.ProductService;
import com.usedcarrot.user.service.UserService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final UserService userService;
    private final ProductService productService;

    public FavoriteService(FavoriteRepository favoriteRepository, UserService userService, ProductService productService) {
        this.favoriteRepository = favoriteRepository;
        this.userService = userService;
        this.productService = productService;
    }

    @Transactional
    public void toggle(CurrentUser currentUser, Long productId) {
        var product = productService.find(productId);
        if (!product.isVisible()) {
            throw new AppException(ErrorCode.NOT_FOUND, "상품을 찾을 수 없습니다.");
        }
        if (product.isSeller(currentUser.getId())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "본인 상품은 관심 등록할 수 없습니다.");
        }
        favoriteRepository.findByUserIdAndProductId(currentUser.getId(), productId)
            .ifPresentOrElse(favoriteRepository::delete,
                () -> favoriteRepository.save(new Favorite(userService.findById(currentUser.getId()), product)));
    }

    @Transactional(readOnly = true)
    public boolean isFavorited(CurrentUser currentUser, Long productId) {
        return favoriteRepository.findByUserIdAndProductId(currentUser.getId(), productId).isPresent();
    }

    @Transactional(readOnly = true)
    public List<Favorite> mine(CurrentUser currentUser) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
    }
}
