package com.trimly.service;

import com.trimly.dto.*;
import com.trimly.entity.RefreshToken;
import com.trimly.entity.Shop;
import com.trimly.entity.User;
import com.trimly.enums.Role;
import com.trimly.enums.ShopStatus;
import com.trimly.exception.TrimlyException;
import com.trimly.repository.RefreshTokenRepository;
import com.trimly.repository.ShopRepository;
import com.trimly.repository.UserRepository;
import com.trimly.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Session strategy:
 *  - Customer: phone → WhatsApp OTP → JWT access token (1 year) + refresh token (10 years)
 *  - Barber/Admin: email+password → same tokens
 *  - Session ends ONLY when user explicitly logs out (refresh token deleted from DB)
 *  - No re-login needed unless they log out or switch devices
 */
@Service @RequiredArgsConstructor @Slf4j
public class AuthService {

    private final UserRepository         userRepo;
    private final ShopRepository         shopRepo;
    private final RefreshTokenRepository rtRepo;
    private final PasswordEncoder        encoder;
    private final JwtService             jwt;
    private final AuthenticationManager  authManager;
//    private final TwilioWhatsAppService whatsApp;
    private final WhatsAppService        whatsApp;

    @Value("${app.platform.otp-expiry-minutes:10}")      private int  otpExpiryMinutes;
    @Value("${app.platform.max-otp-attempts:3}")          private int  maxOtpAttempts;
    @Value("${app.platform.commission-pct:10}")           private int  defaultCommissionPct;
    @Value("${app.jwt.refresh-expiration-ms:315360000000}") private long refreshExpirationMs;

    private final SecureRandom rng = new SecureRandom();

    // ── Customer OTP ──────────────────────────────────────────────────────

    @Transactional
    public void sendCustomerOtp(String phone) {
        User user = userRepo.findByPhone(phone).orElse(null);

        // Rate-limit: block if OTP sent in last 60 seconds
        if (user != null && user.getOtpExpiresAt() != null
                && user.getOtpExpiresAt().isAfter(
                    LocalDateTime.now().plusMinutes(otpExpiryMinutes - 1))) {
            throw TrimlyException.rateLimit("Please wait 60 seconds before requesting another OTP.");
        }

        if (user == null) {
            // First-time user — create stub, name set on first verify
            user = userRepo.save(User.builder()
                .fullName("").phone(phone).role(Role.CUSTOMER).build());
        }

        String code = "123456";//generateOtp();
        user.setOtpCode(encoder.encode(code));
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        user.setOtpAttempts(0);
        userRepo.save(user);

        whatsApp.sendOtp(phone, code, String.valueOf(otpExpiryMinutes));
        log.info("OTP sent to +91{}", phone);
    }

    @Transactional
    public AuthResponse verifyCustomerOtp(VerifyOtpRequest req, String deviceInfo) {
        User user = userRepo.findByPhone(req.getPhone())
            .orElseThrow(() -> TrimlyException.notFound("Phone not found. Request OTP first."));

        if (user.getOtpAttempts() >= maxOtpAttempts)
            throw TrimlyException.rateLimit("Too many attempts. Request a new OTP.");

        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(LocalDateTime.now()))
            throw TrimlyException.badRequest("OTP expired. Request a new one.");

        if (!encoder.matches(req.getOtp(), user.getOtpCode())) {
            user.setOtpAttempts(user.getOtpAttempts() + 1);
            userRepo.save(user);
            int left = maxOtpAttempts - user.getOtpAttempts();
            throw TrimlyException.badRequest("Wrong OTP. " + left + " attempt(s) left.");
        }

        boolean isNew = user.getFullName().isBlank();

        if (req.getFullName() != null && !req.getFullName().isBlank())
            user.setFullName(req.getFullName().trim());
        else if (isNew)
            user.setFullName("User");

        userRepo.clearOtp(user.getId());
        userRepo.save(user);

        log.info("OTP verified for +91{} (new={})", req.getPhone(), isNew);
        return issueTokens(user, null, deviceInfo, isNew);
    }

    // ── Barber / Admin email login ─────────────────────────────────────────

    public AuthResponse login(LoginRequest req, String deviceInfo) {
        String email = req.getEmail().toLowerCase().trim();
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, req.getPassword()));
        } catch (Exception e) {
            throw TrimlyException.unauth("Invalid email or password.");
        }

        User user = userRepo.findByEmail(email)
            .orElseThrow(() -> TrimlyException.notFound("User not found."));

        if (!user.isEnabled())
            throw TrimlyException.forbidden("Account disabled. Contact support@trimly.app.");

        Shop shop = null;
        if (user.getRole() == Role.BARBER) {
            shop = shopRepo.findByOwner_Id(user.getId()).orElse(null);
            if (shop != null && shop.getStatus() == ShopStatus.DISABLED)
                throw TrimlyException.forbidden("Shop disabled. Contact admin@trimly.app.");
        }

        log.info("Login: {} ({})", email, user.getRole());
        return issueTokens(user, shop, deviceInfo, false);
    }

    // ── Refresh — silent token renewal without OTP/password ───────────────

    @Transactional
    public AuthResponse refresh(String rawToken, String deviceInfo) {
        RefreshToken rt = rtRepo.findByToken(rawToken)
            .orElseThrow(() -> TrimlyException.unauth("Session not found. Please login again."));

        if (rt.isExpired()) {
            rtRepo.delete(rt);
            throw TrimlyException.unauth("Session expired. Please login again.");
        }

        User user = rt.getUser();
        if (!user.isEnabled())
            throw TrimlyException.forbidden("Account disabled.");

        rt.setLastUsedAt(LocalDateTime.now());
        rtRepo.save(rt);

        Shop shop = user.getRole() == Role.BARBER
            ? shopRepo.findByOwner_Id(user.getId()).orElse(null) : null;

        // New access token, same refresh token — stays alive until logout
        return AuthResponse.builder()
            .accessToken(jwt.generateAccessToken(user))
            .refreshToken(rawToken)
            .accessTokenExpiresIn(jwt.getAccessExpirationMs())
            .refreshTokenExpiresIn(
                java.time.Duration.between(LocalDateTime.now(), rt.getExpiresAt()).toMillis())
            .isNewUser(false)
            .user(buildUserInfo(user, shop))
            .build();
    }

    // ── Logout — the ONLY way a session ends ──────────────────────────────

    @Transactional
    public void logout(Long userId, LogoutRequest req) {
        if (req != null && req.isAllDevices()) {
            rtRepo.deleteAllByUserId(userId);
            log.info("User {} logged out from all devices", userId);
        } else if (req != null && req.getRefreshToken() != null) {
            rtRepo.deleteByToken(req.getRefreshToken());
            log.info("User {} logged out from current device", userId);
        } else {
            // Fallback — logout all
            rtRepo.deleteAllByUserId(userId);
        }
    }

    // ── Barber registration ────────────────────────────────────────────────

    @Transactional
    public AuthResponse registerBarber(BarberRegisterRequest req, String deviceInfo) {
        String email = req.getEmail().toLowerCase().trim();

        if (userRepo.existsByEmail(email))
            throw TrimlyException.conflict("Email already registered.");
        if (userRepo.existsByPhone(req.getPhone()))
            throw TrimlyException.conflict("Phone already registered.");

        User user = userRepo.save(User.builder()
            .fullName(req.getFullName().trim())
            .email(email)
            .password(encoder.encode(req.getPassword()))
            .phone(req.getPhone())
            .role(Role.BARBER)
            .build());

        Shop shop = shopRepo.save(Shop.builder()
            .shopName(req.getShopName().trim())
            .slug(makeSlug(req.getShopName()))
            .location(req.getLocation().trim())
            .city(req.getCity() != null ? req.getCity().trim() : null)
            .area(req.getArea() != null ? req.getArea().trim() : null)
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .commissionPercent(java.math.BigDecimal.valueOf(defaultCommissionPct))
            .owner(user)
            .status(ShopStatus.PENDING)
            .build());

        log.info("Barber registered: {} — shop '{}' pending", email, shop.getShopName());
        return issueTokens(user, shop, deviceInfo, true);
    }

    // ── Password reset (barbers only) ──────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        User user = userRepo.findByEmail(req.getEmail().toLowerCase().trim())
            .orElseThrow(() -> TrimlyException.notFound("No account with that email."));

        if (user.getRole() == Role.CUSTOMER)
            throw TrimlyException.badRequest("Customers use WhatsApp OTP — no password needed.");

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiresAt(LocalDateTime.now().plusHours(2));
        userRepo.save(user);

        String link = "https://trimly.app/reset-password?token=" + token;
        whatsApp.sendPasswordReset(user.getPhone(), user.getFullName(), link, "2");
        log.info("Password reset sent to {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        User user = userRepo.findByResetToken(req.getToken())
            .orElseThrow(() -> TrimlyException.badRequest("Invalid or expired reset link."));

        if (user.getResetTokenExpiresAt().isBefore(LocalDateTime.now()))
            throw TrimlyException.badRequest("Reset link expired. Request a new one.");

        user.setPassword(encoder.encode(req.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiresAt(null);
        // Invalidate all sessions on password change for security
        rtRepo.deleteAllByUserId(user.getId());
        userRepo.save(user);
        log.info("Password reset for {}", user.getEmail());
    }

    // ── Scheduled cleanup (runs 3 AM daily) ───────────────────────────────

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        rtRepo.deleteExpiredBefore(LocalDateTime.now());
        log.info("Refresh token cleanup complete");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private AuthResponse issueTokens(User user, Shop shop, String deviceInfo, boolean isNew) {
        String rawRefresh = generateRefreshToken();
        rtRepo.save(RefreshToken.builder()
            .user(user)
            .token(rawRefresh)
            .deviceInfo(deviceInfo)
            .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
            .lastUsedAt(LocalDateTime.now())
            .build());

        return AuthResponse.builder()
            .accessToken(jwt.generateAccessToken(user))
            .refreshToken(rawRefresh)
            .accessTokenExpiresIn(jwt.getAccessExpirationMs())
            .refreshTokenExpiresIn(refreshExpirationMs)
            .isNewUser(isNew)
            .user(buildUserInfo(user, shop))
            .build();
    }

    private UserInfo buildUserInfo(User user, Shop shop) {
        return UserInfo.builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .role(user.getRole())
            .shopId(shop != null ? shop.getId() : null)
            .shopName(shop != null ? shop.getShopName() : null)
            .shopStatus(shop != null ? shop.getStatus() : null)
            .build();
    }

    private String generateOtp() {
        return String.format("%06d", rng.nextInt(1_000_000));
    }

    private String generateRefreshToken() {
        byte[] b = new byte[48];
        rng.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private String makeSlug(String name) {
        String base = name.toLowerCase()
            .replaceAll("[^a-z0-9]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        String slug = base;
        int i = 1;
        while (shopRepo.existsBySlug(slug)) slug = base + "-" + (i++);
        return slug;
    }
}
