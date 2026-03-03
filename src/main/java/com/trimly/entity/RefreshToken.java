package com.trimly.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Persistent refresh token — stored in DB per device/session.
 * When access token expires, client sends refresh token to get a new access token
 * WITHOUT asking for OTP/password again.
 *
 * Session lifetime controlled by app.jwt.refresh-expiration-ms (default 30 days).
 * On explicit logout, the refresh token is deleted → session truly ends.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_rt_token", columnList = "token", unique = true),
    @Index(name = "idx_rt_user",  columnList = "user_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The opaque token string sent to client (stored hashed) */
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    /** Human-readable device hint (e.g. "Chrome on Android") */
    @Column(length = 200)
    private String deviceInfo;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime lastUsedAt;

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }
}
