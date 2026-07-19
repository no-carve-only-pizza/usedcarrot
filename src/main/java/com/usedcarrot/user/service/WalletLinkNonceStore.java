package com.usedcarrot.user.service;

import com.usedcarrot.common.AppException;
import com.usedcarrot.common.ErrorCode;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class WalletLinkNonceStore {
    private static final String NONCE_KEY = "walletLinkNonce";
    private static final String ISSUED_KEY = "walletLinkNonceIssuedAt";
    private static final Duration TTL = Duration.ofMinutes(10);

    private WalletLinkNonceStore() {
    }

    public static String issue(HttpSession session) {
        String nonce = UUID.randomUUID().toString();
        session.setAttribute(NONCE_KEY, nonce);
        session.setAttribute(ISSUED_KEY, Instant.now().toString());
        return nonce;
    }

    public static String requireValid(HttpSession session, String messageNonce) {
        Object stored = session.getAttribute(NONCE_KEY);
        Object issuedAt = session.getAttribute(ISSUED_KEY);
        clear(session);
        if (stored == null || issuedAt == null || messageNonce == null) {
            throw new AppException(ErrorCode.BAD_REQUEST, "지갑 연결 세션이 만료되었습니다. 새로고침 후 다시 시도하세요.");
        }
        Instant issued = Instant.parse(issuedAt.toString());
        if (issued.plus(TTL).isBefore(Instant.now())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "지갑 연결 세션이 만료되었습니다. 새로고침 후 다시 시도하세요.");
        }
        if (!stored.toString().equals(messageNonce)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "지갑 연결 nonce가 일치하지 않습니다.");
        }
        return stored.toString();
    }

    public static void clear(HttpSession session) {
        session.removeAttribute(NONCE_KEY);
        session.removeAttribute(ISSUED_KEY);
    }

    public static String extractNonce(String message) {
        String marker = "|nonce=";
        int idx = message.indexOf(marker);
        if (idx < 0) {
            return null;
        }
        return message.substring(idx + marker.length()).trim();
    }
}
