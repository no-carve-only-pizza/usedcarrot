package com.usedcarrot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

public class LoginRateLimitFilter extends OncePerRequestFilter {
    private final LoginRateLimitService rateLimitService;
    private final ClientIpResolver clientIpResolver;

    public LoginRateLimitFilter(LoginRateLimitService rateLimitService, ClientIpResolver clientIpResolver) {
        this.rateLimitService = rateLimitService;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (HttpMethod.POST.matches(request.getMethod()) && "/login".equals(request.getServletPath())
            && rateLimitService.isBlocked(clientIpResolver.resolve(request))) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("로그인 요청이 너무 많습니다. 잠시 후 다시 시도하세요.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
