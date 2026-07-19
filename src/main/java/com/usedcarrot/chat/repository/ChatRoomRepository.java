package com.usedcarrot.chat.repository;

import com.usedcarrot.chat.domain.ChatRoom;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByProductIdAndBuyerIdAndSellerId(Long productId, Long buyerId, Long sellerId);

    @Query("""
        select r from ChatRoom r
        join fetch r.product
        join fetch r.buyer
        join fetch r.seller
        where r.id = :id
        """)
    Optional<ChatRoom> findByIdWithParticipants(@Param("id") Long id);

    @Query("""
        select r from ChatRoom r
        join fetch r.product
        join fetch r.buyer
        join fetch r.seller
        where r.buyer.id = :userId or r.seller.id = :userId
        order by r.updatedAt desc
        """)
    List<ChatRoom> findMine(@Param("userId") Long userId);
}
