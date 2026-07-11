package com.usedcarrot.chat.service;

import com.usedcarrot.audit.domain.AuditEventType;
import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.chat.domain.ChatRoom;
import com.usedcarrot.chat.domain.Message;
import com.usedcarrot.chat.repository.ChatRoomRepository;
import com.usedcarrot.chat.repository.MessageRepository;
import com.usedcarrot.common.AppException;
import com.usedcarrot.common.AuditLogger;
import com.usedcarrot.common.ErrorCode;
import com.usedcarrot.product.domain.Product;
import com.usedcarrot.product.service.ProductService;
import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserStatus;
import com.usedcarrot.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final ProductService productService;
    private final UserService userService;
    private final AuditLogger auditLogger;

    public ChatService(ChatRoomRepository chatRoomRepository, MessageRepository messageRepository,
                       ProductService productService, UserService userService, AuditLogger auditLogger) {
        this.chatRoomRepository = chatRoomRepository;
        this.messageRepository = messageRepository;
        this.productService = productService;
        this.userService = userService;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public ChatRoom createOrGet(CurrentUser currentUser, Long productId) {
        User buyer = userService.findById(currentUser.getId());
        if (buyer.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "제한된 사용자는 채팅을 시작할 수 없습니다.");
        }
        Product product = productService.find(productId);
        if (!product.isPurchasable()) {
            throw new AppException(ErrorCode.INVALID_STATE, "거래 가능한 상품만 문의할 수 있습니다.");
        }
        User seller = product.getSeller();
        if (seller.getId().equals(buyer.getId())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "본인 상품에는 문의할 수 없습니다.");
        }
        return chatRoomRepository.findByProductIdAndBuyerIdAndSellerId(product.getId(), buyer.getId(), seller.getId())
            .orElseGet(() -> chatRoomRepository.save(new ChatRoom(product, buyer, seller)));
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> mine(CurrentUser currentUser) {
        return chatRoomRepository.findMine(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public ChatRoom room(Long roomId, CurrentUser currentUser) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "채팅방을 찾을 수 없습니다."));
        if (!room.isParticipant(currentUser.getId()) && !isAdmin(currentUser)) {
            throw new AccessDeniedException("forbidden");
        }
        return room;
    }

    @Transactional(readOnly = true)
    public List<Message> messages(Long roomId, CurrentUser currentUser) {
        room(roomId, currentUser);
        return messageRepository.findByChatRoomIdOrderByCreatedAtAsc(roomId);
    }

    @Transactional
    public void send(Long roomId, CurrentUser currentUser, String content, HttpServletRequest request) {
        ChatRoom room = room(roomId, currentUser);
        User sender = userService.findById(currentUser.getId());
        if (sender.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.ACCESS_DENIED, "제한된 사용자는 메시지를 보낼 수 없습니다.");
        }
        messageRepository.save(new Message(room, sender, content));
        auditLogger.log(sender.getId(), AuditEventType.MESSAGE_SENT, "SUCCESS", "roomId=" + roomId, request);
    }

    private boolean isAdmin(CurrentUser currentUser) {
        return currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
