package com.usedcarrot.product.repository;

import com.usedcarrot.product.domain.ProductImage;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    @Query("select i from ProductImage i join fetch i.product p join fetch p.seller where i.storedFileName = :storedFileName")
    Optional<ProductImage> findByStoredFileNameWithProduct(@Param("storedFileName") String storedFileName);
}
