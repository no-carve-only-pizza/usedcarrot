package com.usedcarrot.chat.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.chat.dto.MessageCreateRequest;
import com.usedcarrot.chat.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public String create(@AuthenticationPrincipal CurrentUser currentUser, @RequestParam Long productId) {
        return "redirect:/chat/" + chatService.createOrGet(currentUser, productId).getId();
    }

    @GetMapping("/chat")
    public String list(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        model.addAttribute("rooms", chatService.mine(currentUser));
        return "chat/list";
    }

    @GetMapping("/chat/{roomId}")
    public String room(@PathVariable Long roomId, @AuthenticationPrincipal CurrentUser currentUser, Model model) {
        model.addAttribute("room", chatService.room(roomId, currentUser));
        model.addAttribute("messages", chatService.messages(roomId, currentUser));
        model.addAttribute("messageCreateRequest", new MessageCreateRequest());
        model.addAttribute("idempotencyKey", UUID.randomUUID().toString());
        return "chat/room";
    }

    @PostMapping("/chat/{roomId}/messages")
    public String send(@PathVariable Long roomId, @AuthenticationPrincipal CurrentUser currentUser,
                       @Valid MessageCreateRequest request, BindingResult bindingResult,
                       HttpServletRequest servletRequest) {
        if (!bindingResult.hasErrors()) {
            chatService.send(roomId, currentUser, request.getContent(), servletRequest);
        }
        return "redirect:/chat/" + roomId;
    }
}
