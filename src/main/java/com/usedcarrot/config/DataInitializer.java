package com.usedcarrot.config;

import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserRole;
import com.usedcarrot.user.repository.UserRepository;
import com.usedcarrot.wallet.service.WalletService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder, WalletService walletService) {
        return args -> {
            if (!userRepository.existsByEmail("admin@usedcarrot.test")) {
                User admin = userRepository.save(new User("admin@usedcarrot.test", passwordEncoder.encode("Admin123!"),
                    "관리자", "서울", UserRole.ROLE_ADMIN));
                walletService.createInitialWallet(admin, null);
            }
            if (!userRepository.existsByEmail("user@usedcarrot.test")) {
                User user = userRepository.save(new User("user@usedcarrot.test", passwordEncoder.encode("User1234!"),
                    "일반유저", "서울 강남구", UserRole.ROLE_USER));
                walletService.createInitialWallet(user, null);
            }
        };
    }
}
