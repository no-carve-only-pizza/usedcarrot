package com.usedcarrot.chat.repository;

import com.usedcarrot.chat.domain.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    List<Message> findByChatRoomIdAndIdGreaterThanOrderByIdAsc(Long chatRoomId, Long afterId);
}
