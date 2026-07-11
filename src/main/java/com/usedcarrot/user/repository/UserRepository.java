package com.usedcarrot.user.repository;

import com.usedcarrot.user.domain.User;
import com.usedcarrot.user.domain.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndIdNot(String nickname, Long id);
    long countByStatus(UserStatus status);
    List<User> findByEmailContainingIgnoreCaseOrNicknameContainingIgnoreCase(String email, String nickname);
}
