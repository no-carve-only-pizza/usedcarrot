package com.usedcarrot.chat.controller;

import com.usedcarrot.auth.service.CurrentUser;
import com.usedcarrot.chat.domain.Message;
import com.usedcarrot.chat.dto.MessageCreateRequest;
import com.usedcarrot.chat.dto.MessageView;
import com.usedcarrot.chat.service.ChatService;
import com.usedcarrot.common.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ChatController {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public String create(@AuthenticationPrincipal CurrentUser currentUser, @RequestParam Long productId,
                         RedirectAttributes redirectAttributes) {
        try {
            return "redirect:/chat/" + chatService.createOrGet(currentUser, productId).getId();
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/products/" + productId;
        }
    }

    @GetMapping("/chat")
    public String list(@AuthenticationPrincipal CurrentUser currentUser, Model model) {
        model.addAttribute("rooms", chatService.mine(currentUser));
        return "chat/list";
    }

    @GetMapping("/chat/{roomId}")
    public String room(@PathVariable Long roomId, @AuthenticationPrincipal CurrentUser currentUser, Model model) {
        var room = chatService.room(roomId, currentUser);
        boolean buyer = room.getBuyer().getId().equals(currentUser.getId());
        model.addAttribute("room", room);
        model.addAttribute("messages", chatService.messages(roomId, currentUser));
        model.addAttribute("messageCreateRequest", new MessageCreateRequest());
        model.addAttribute("idempotencyKey", UUID.randomUUID().toString());
        model.addAttribute("canSend", room.getProduct().canAcceptMessages());
        model.addAttribute("canPurchase", buyer && room.getProduct().isPurchasable());
        model.addAttribute("currentUserId", currentUser.getId());
        return "chat/room";
    }

    @GetMapping("/chat/{roomId}/messages.json")
    @ResponseBody
    public List<MessageView> poll(@PathVariable Long roomId,
                                  @RequestParam(defaultValue = "0") Long afterId,
                                  @AuthenticationPrincipal CurrentUser currentUser) {
        return chatService.messagesAfter(roomId, currentUser, afterId).stream()
            .map(this::toView)
            .toList();
    }

    @PostMapping("/chat/{roomId}/messages.json")
    @ResponseBody
    public ResponseEntity<?> sendJson(@PathVariable Long roomId,
                                      @AuthenticationPrincipal CurrentUser currentUser,
                                      @Valid MessageCreateRequest request,
                                      BindingResult bindingResult,
                                      HttpServletRequest servletRequest) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", "메시지 내용을 확인해주세요."));
        }
        try {
            Message saved = chatService.send(roomId, currentUser, request.getContent(), servletRequest);
            return ResponseEntity.ok(toView(saved));
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/chat/{roomId}/messages")
    public String send(@PathVariable Long roomId, @AuthenticationPrincipal CurrentUser currentUser,
                       @Valid MessageCreateRequest request, BindingResult bindingResult,
                       HttpServletRequest servletRequest, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "메시지 내용을 확인해주세요.");
            return "redirect:/chat/" + roomId;
        }
        try {
            chatService.send(roomId, currentUser, request.getContent(), servletRequest);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/chat/" + roomId;
    }

    private MessageView toView(Message message) {
        return new MessageView(
            message.getId(),
            message.getSender().getId(),
            message.getSender().getNickname(),
            message.getContent(),
            message.getCreatedAt().format(TIME_FMT)
        );
    }
}
