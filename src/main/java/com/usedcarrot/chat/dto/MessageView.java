package com.usedcarrot.chat.dto;

public record MessageView(Long id, Long senderId, String senderNickname, String content, String createdAt) {
}
