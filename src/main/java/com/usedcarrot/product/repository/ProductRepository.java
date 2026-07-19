package com.usedcarrot.product.repository;

import com.usedcarrot.product.domain.Product;
import com.usedcarrot.product.domain.ProductStatus;
import com.usedcarrot.user.domain.UserStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @Query("""
        select p from Product p join fetch p.seller s
        where p.status not in (com.usedcarrot.product.domain.ProductStatus.HIDDEN, com.usedcarrot.product.domain.ProductStatus.DELETED)
          and s.status = com.usedcarrot.user.domain.UserStatus.ACTIVE
          and (
                (:status is null and p.status in (com.usedcarrot.product.domain.ProductStatus.ON_SALE,
                                                  com.usedcarrot.product.domain.ProductStatus.RESERVED))
             or (:status is not null and p.status = :status)
          )
          and (:keyword is null or lower(p.title) like lower(concat('%', :keyword, '%'))
               or lower(p.description) like lower(concat('%', :keyword, '%')))
          and (:category is null or p.category = :category)
          and (:region is null or lower(p.region) like lower(concat('%', :region, '%')))
        """)
    Page<Product> searchVisible(@Param("keyword") String keyword, @Param("category") String category,
                                @Param("status") ProductStatus status, @Param("region") String region, Pageable pageable);

    @Query("""
        select p from Product p join fetch p.seller s
        where p.status = com.usedcarrot.product.domain.ProductStatus.ON_SALE
          and s.status = com.usedcarrot.user.domain.UserStatus.ACTIVE
        order by p.createdAt desc
        """)
    List<Product> findLatestVisible(Pageable pageable);

    @EntityGraph(attributePaths = {"seller", "images"})
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdWithSellerAndImages(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p join fetch p.seller where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    List<Product> findBySellerIdOrderByCreatedAtDesc(Long sellerId);

    boolean existsBySellerIdAndStatusIn(Long sellerId, Collection<ProductStatus> statuses);

    long countByStatus(ProductStatus status);
}
