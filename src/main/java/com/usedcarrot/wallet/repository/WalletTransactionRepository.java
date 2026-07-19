package com.usedcarrot.wallet.repository;

import com.usedcarrot.wallet.domain.WalletTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);
    Optional<WalletTransaction> findByTransactionHash(String transactionHash);

    @Query("""
        select t from WalletTransaction t
        left join fetch t.product
        left join fetch t.buyer
        left join fetch t.seller
        where t.buyer.id = :userId or t.seller.id = :userId
        order by t.createdAt desc
        """)
    List<WalletTransaction> findMine(@Param("userId") Long userId);
}
