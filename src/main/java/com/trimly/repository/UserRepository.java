package com.trimly.repository;

import com.trimly.entity.User;
import com.trimly.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByResetToken(String token);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    long countByRole(Role role);

    @Modifying
    @Query("UPDATE User u SET u.otpCode = null, u.otpExpiresAt = null, u.otpAttempts = 0 WHERE u.id = :id")
    void clearOtp(@Param("id") Long id);
}
