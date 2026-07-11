package com.usedcarrot.favorite.service;

import com.usedcarrot.auth.service.CurrentUser;
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
        favoriteRepository.findByUserIdAndProductId(currentUser.getId(), productId)
            .ifPresentOrElse(favoriteRepository::delete,
                () -> favoriteRepository.save(new Favorite(userService.findById(currentUser.getId()), productService.find(productId))));
    }

    @Transactional(readOnly = true)
    public List<Favorite> mine(CurrentUser currentUser) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
    }
}
