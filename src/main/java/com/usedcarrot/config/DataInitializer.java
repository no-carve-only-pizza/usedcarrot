package com.usedcarrot.config;

import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserRole;
import com.usedcarrot.user.domain.UserStatus;
import com.usedcarrot.user.repository.UserRepository;
import com.usedcarrot.wallet.service.WalletService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder, WalletService walletService,
                                @Value("${usedcarrot.bootstrap-admin.email:}") String adminEmail,
                                @Value("${usedcarrot.bootstrap-admin.password:}") String adminPassword) {
        return args -> {
            suspendLegacyDefaultAccount(userRepository, "admin@usedcarrot.test");
            suspendLegacyDefaultAccount(userRepository, "user@usedcarrot.test");
            if (adminEmail.isBlank() && adminPassword.isBlank()) {
                return;
            }
            if (adminEmail.isBlank() || adminPassword.length() < 12) {
                throw new IllegalStateException("관리자 이메일과 12자 이상의 비밀번호를 모두 설정해야 합니다.");
            }
            if (!userRepository.existsByEmail(adminEmail)) {
                String nickname = "관리자-" + Integer.toHexString(adminEmail.hashCode());
                User admin = userRepository.save(new User(adminEmail, passwordEncoder.encode(adminPassword),
                    nickname, "관리자 지역", UserRole.ROLE_ADMIN));
                walletService.createWallet(admin, null);
            }
        };
    }

    private void suspendLegacyDefaultAccount(UserRepository userRepository, String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.changeStatus(UserStatus.SUSPENDED);
            userRepository.save(user);
        });
    }
}
