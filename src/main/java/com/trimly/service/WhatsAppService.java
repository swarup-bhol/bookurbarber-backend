package com.trimly.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * WhatsApp Business Cloud API integration (Meta Graph API v19.0).
 *
 * All messages are sent asynchronously so they never block HTTP responses.
 *
 * SETUP GUIDE:
 * 1. Go to https://developers.facebook.com → Create App → Add "WhatsApp"
 * 2. Create message templates in Meta Business Manager (templates take 24–48h to approve)
 * 3. Set WA_ENABLED=true and fill WA_PHONE_NUMBER_ID + WA_ACCESS_TOKEN in secrets file
 *
 * Phone numbers: always stored/sent as 10-digit Indian numbers, prefixed with "91" for API.
 */
@Service @Slf4j
public class WhatsAppService {

    @Value("${app.whatsapp.enabled:false}")           private boolean enabled;
    @Value("${app.whatsapp.base-url}")                private String baseUrl;
    @Value("${app.whatsapp.phone-number-id:}")        private String phoneNumberId;
    @Value("${app.whatsapp.access-token:}")           private String accessToken;
    @Value("${app.whatsapp.language:en_IN}")          private String language;

    // Template names (must match approved templates in Meta Business Manager)
    @Value("${app.whatsapp.template.booking-request:trimly_booking_request}")     private String tplBookingRequest;
    @Value("${app.whatsapp.template.booking-confirmed:trimly_booking_confirmed}") private String tplBookingConfirmed;
    @Value("${app.whatsapp.template.booking-rejected:trimly_booking_rejected}")   private String tplBookingRejected;
    @Value("${app.whatsapp.template.booking-cancelled:trimly_booking_cancelled}") private String tplBookingCancelled;
    @Value("${app.whatsapp.template.booking-completed:trimly_booking_completed}") private String tplBookingCompleted;
    @Value("${app.whatsapp.template.reschedule-request:trimly_reschedule_request}") private String tplRescheduleRequest;
    @Value("${app.whatsapp.template.reschedule-response:trimly_reschedule_response}") private String tplRescheduleResponse;
    @Value("${app.whatsapp.template.otp-login:trimly_otp_login}")                private String tplOtp;
    @Value("${app.whatsapp.template.password-reset:trimly_password_reset}")       private String tplPasswordReset;

    private final WebClient webClient;

    public WhatsAppService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public messaging methods (all @Async — non-blocking)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Notify barber of a new booking request.
     * Template: trimly_booking_request
     * Params: {{1}}=customerName, {{2}}=services, {{3}}=date, {{4}}=time, {{5}}=bookingRef
     */
    @Async
    public void sendBookingRequestToBarber(String barberPhone, String customerName,
            String services, String date, String time, String bookingRef) {
        send(barberPhone, tplBookingRequest,
            params(customerName, services, date, time, bookingRef));
    }

    /**
     * Notify customer that their booking is confirmed.
     * Template: trimly_booking_confirmed
     * Params: {{1}}=customerName, {{2}}=shopName, {{3}}=services, {{4}}=date, {{5}}=time
     */
    @Async
    public void sendBookingConfirmedToCustomer(String customerPhone, String customerName,
            String shopName, String services, String date, String time) {
        send(customerPhone, tplBookingConfirmed,
            params(customerName, shopName, services, date, time));
    }

    /**
     * Notify customer that their booking is rejected.
     * Template: trimly_booking_rejected
     * Params: {{1}}=customerName, {{2}}=shopName, {{3}}=services, {{4}}=reason
     */
    @Async
    public void sendBookingRejectedToCustomer(String customerPhone, String customerName,
            String shopName, String services, String reason) {
        send(customerPhone, tplBookingRejected,
            params(customerName, shopName, services, reason != null ? reason : "No reason provided"));
    }

    /**
     * Notify of cancellation (send to whoever didn't cancel).
     * Template: trimly_booking_cancelled
     * Params: {{1}}=name, {{2}}=shopName, {{3}}=date, {{4}}=time
     */
    @Async
    public void sendCancellationNotice(String toPhone, String name,
            String shopName, String date, String time) {
        send(toPhone, tplBookingCancelled, params(name, shopName, date, time));
    }

    /**
     * Notify customer of completed booking with review prompt.
     * Template: trimly_booking_completed
     * Params: {{1}}=customerName, {{2}}=shopName, {{3}}=amount
     */
    @Async
    public void sendBookingCompleted(String customerPhone, String customerName,
            String shopName, String amount) {
        send(customerPhone, tplBookingCompleted, params(customerName, shopName, amount));
    }

    /**
     * Barber requests reschedule — notify customer.
     * Template: trimly_reschedule_request
     * Params: {{1}}=customerName, {{2}}=shopName, {{3}}=oldTime, {{4}}=newDate, {{5}}=newTime, {{6}}=reason
     */
    @Async
    public void sendRescheduleRequestToCustomer(String customerPhone, String customerName,
            String shopName, String oldTime, String newDate, String newTime, String reason) {
        send(customerPhone, tplRescheduleRequest,
            params(customerName, shopName, oldTime, newDate, newTime, reason));
    }

    /**
     * Customer responds to reschedule — notify barber.
     * Template: trimly_reschedule_response
     * Params: {{1}}=shopName, {{2}}=customerName, {{3}}=newTime, {{4}}=status
     */
    @Async
    public void sendRescheduleResponseToBarber(String barberPhone, String shopName,
            String customerName, String newTime, String status) {
        send(barberPhone, tplRescheduleResponse,
            params(shopName, customerName, newTime, status));
    }

    /**
     * Send OTP to customer for WhatsApp-based login.
     * Template: trimly_otp_login
     * Params: {{1}}=otp, {{2}}=expiryMinutes
     */
    @Async
    public void sendOtp(String phone, String otp, String expiryMinutes) {
        send(phone, tplOtp, params(otp, expiryMinutes));
    }

    /**
     * Send password reset link to barber.
     * Template: trimly_password_reset
     * Params: {{1}}=name, {{2}}=resetLink, {{3}}=expiryHours
     */
    @Async
    public void sendPasswordReset(String phone, String name, String resetLink, String expiryHours) {
        send(phone, tplPasswordReset, params(name, resetLink, expiryHours));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core send logic
    // ─────────────────────────────────────────────────────────────────────────

    private void send(String phone, String templateName, List<Map<String, Object>> parameters) {
        if (!enabled) {
            log.info("[WA-MOCK] to=+91{} template={} params={}", phone, templateName, parameters);
            return;
        }

        if (phoneNumberId.isBlank() || accessToken.isBlank()) {
            log.warn("WhatsApp credentials not configured — skipping message to +91{}", phone);
            return;
        }

        // Normalize phone: strip leading 0/+91, keep 10 digits, prepend country code 91
        String normalised = "91" + phone.replaceAll("^(\\+91|91|0)", "").replaceAll("\\D", "");

        Map<String, Object> body = Map.of(
            "messaging_product", "whatsapp",
            "to", normalised,
            "type", "template",
            "template", Map.of(
                "name", templateName,
                "language", Map.of("code", language),
                "components", List.of(
                    Map.of(
                        "type", "body",
                        "parameters", parameters
                    )
                )
            )
        );

        webClient.post()
            .uri(baseUrl + "/" + phoneNumberId + "/messages")
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .doOnSuccess(resp -> log.info("[WA] Sent template={} to=+91{} response={}", templateName, phone, resp))
            .doOnError(e -> {
                if (e instanceof WebClientResponseException wce) {
                    log.error("[WA] Failed template={} to=+91{} status={} body={}",
                        templateName, phone, wce.getStatusCode(), wce.getResponseBodyAsString());
                } else {
                    log.error("[WA] Failed template={} to=+91{} error={}", templateName, phone, e.getMessage());
                }
            })
            .subscribe(); // fire and forget
    }

    /** Build a list of text parameters for a WhatsApp template */
    private List<Map<String, Object>> params(String... values) {
        return java.util.Arrays.stream(values)
            .map(v -> Map.<String, Object>of("type", "text", "text", v != null ? v : ""))
            .toList();
    }
}
