package com.usedcarrot.common;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AppException.class)
    public String appException(AppException e, Model model, HttpServletResponse response) {
        response.setStatus(statusFor(e.getErrorCode()).value());
        model.addAttribute("message", e.getMessage());
        model.addAttribute("code", e.getErrorCode().name());
        return "error/error";
    }

    private HttpStatus statusFor(ErrorCode errorCode) {
        return switch (errorCode) {
            case BAD_REQUEST, FILE_UPLOAD_REJECTED -> HttpStatus.BAD_REQUEST;
            case ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_RESOURCE, INVALID_STATE -> HttpStatus.CONFLICT;
        };
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public String accessDenied(Model model) {
        model.addAttribute("message", "접근 권한이 없습니다.");
        model.addAttribute("code", "ACCESS_DENIED");
        return "error/error";
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public String unexpected(Model model) {
        model.addAttribute("message", "요청 처리 중 오류가 발생했습니다.");
        model.addAttribute("code", "INTERNAL_ERROR");
        return "error/error";
    }
}
