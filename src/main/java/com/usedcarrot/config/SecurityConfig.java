package com.usedcarrot.config;

import com.usedcarrot.audit.domain.AuditEventType;
import com.usedcarrot.common.AuditLogger;
import com.usedcarrot.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationSuccessHandler successHandler,
                                            AuthenticationFailureHandler failureHandler, AuditLogger auditLogger) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/error", "/products", "/products/*", "/login", "/register", "/css/**", "/uploads/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/reports/new").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .successHandler(successHandler)
                .failureHandler(failureHandler)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    Long userId = userId(authentication);
                    auditLogger.log(userId, AuditEventType.LOGOUT, "SUCCESS", "logout", request);
                    response.sendRedirect("/login?logout");
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            .sessionManagement(session -> session.sessionFixation().migrateSession())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Bean
    AuthenticationSuccessHandler successHandler(UserRepository userRepository, AuditLogger auditLogger) {
        return (request, response, authentication) -> {
            userRepository.findByEmail(authentication.getName()).ifPresent(user -> {
                user.loginSucceeded();
                userRepository.save(user);
                auditLogger.log(user.getId(), AuditEventType.LOGIN_SUCCESS, "SUCCESS", "login", request);
            });
            response.sendRedirect("/");
        };
    }

    @Bean
    AuthenticationFailureHandler failureHandler(UserRepository userRepository, AuditLogger auditLogger) {
        return (HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response,
                AuthenticationException exception) -> {
            String email = request.getParameter("email");
            userRepository.findByEmail(email).ifPresent(user -> {
                user.loginFailed();
                userRepository.save(user);
                auditLogger.log(user.getId(), AuditEventType.LOGIN_FAILURE, "FAIL", "login failure", request);
            });
            response.sendRedirect("/login?error");
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    private Long userId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.usedcarrot.auth.service.CurrentUser currentUser) {
            return currentUser.getId();
        }
        return null;
    }
}
