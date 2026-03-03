package com.trimly.entity;

import com.trimly.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email",  columnList = "email",  unique = true),
    @Index(name = "idx_user_phone",  columnList = "phone",  unique = true),
    @Index(name = "idx_user_role",   columnList = "role")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity implements UserDetails {

    @Column(nullable = false, length = 100)
    private String fullName;

    // Email is optional for customers (they use phone OTP)
    @Column(unique = true, length = 150)
    private String email;

    @Column
    private String password;

    /** 10-digit Indian mobile number (no country code stored — always +91) */
    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    // ── OTP fields (for customer WhatsApp OTP login) ──────────────────────
    @Column(length = 60)
    private String otpCode;

    @Column
    private LocalDateTime otpExpiresAt;

    @Column(nullable = false)
    @Builder.Default
    private int otpAttempts = 0;

    // ── Password reset (for barbers) ─────────────────────────────────────
    @Column(length = 200)
    private String resetToken;

    @Column
    private LocalDateTime resetTokenExpiresAt;

    // ── Relationships ─────────────────────────────────────────────────────
    @OneToOne(mappedBy = "owner", fetch = FetchType.LAZY)
    private Shop shop;

    // ── UserDetails ───────────────────────────────────────────────────────
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getUsername()               { return email != null ? email : phone; }
    @Override public boolean isAccountNonExpired()      { return true; }
    @Override public boolean isAccountNonLocked()       { return true; }
    @Override public boolean isCredentialsNonExpired()  { return true; }
}
