package com.usedcarrot.favorite.repository;

import com.usedcarrot.favorite.domain.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);
    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
}
