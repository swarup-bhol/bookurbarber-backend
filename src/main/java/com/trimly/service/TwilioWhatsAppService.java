package com.trimly.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TwilioWhatsAppService {

    @Value("${app.whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${app.whatsapp.twilio.account-sid}")
    private String accountSid;

    @Value("${app.whatsapp.twilio.auth-token}")
    private String authToken;

    @Value("${app.whatsapp.twilio.sandbox-number}")
    private String sandboxNumber;

    public TwilioWhatsAppService(
            @Value("${app.whatsapp.twilio.account-sid}") String accountSid,
            @Value("${app.whatsapp.twilio.auth-token}") String authToken) {

        Twilio.init(accountSid, authToken);
    }

    // ─────────────────────────────────────────
    // Booking notifications
    // ─────────────────────────────────────────

    @Async
    public void sendBookingRequestToBarber(String barberPhone,
                                           String customerName,
                                           String services,
                                           String date,
                                           String time,
                                           String bookingRef) {

        String msg = String.format(
                "New Booking Request\nCustomer: %s\nService: %s\nDate: %s\nTime: %s\nRef: %s",
                customerName, services, date, time, bookingRef);

        send(barberPhone, msg);
    }

    @Async
    public void sendBookingConfirmedToCustomer(String phone,
                                               String customerName,
                                               String shopName,
                                               String services,
                                               String date,
                                               String time) {

        String msg = String.format(
                "Hi %s, your booking at %s for %s on %s at %s is confirmed.",
                customerName, shopName, services, date, time);

        send(phone, msg);
    }

    @Async
    public void sendBookingRejectedToCustomer(String phone,
                                              String customerName,
                                              String shopName,
                                              String services,
                                              String reason) {

        String msg = String.format(
                "Hi %s, your booking at %s for %s was rejected. Reason: %s",
                customerName, shopName, services,
                reason != null ? reason : "No reason provided");

        send(phone, msg);
    }

    @Async
    public void sendCancellationNotice(String phone,
                                       String name,
                                       String shopName,
                                       String date,
                                       String time) {

        String msg = String.format(
                "Hello %s, your booking at %s on %s at %s has been cancelled.",
                name, shopName, date, time);

        send(phone, msg);
    }

    @Async
    public void sendBookingCompleted(String phone,
                                     String customerName,
                                     String shopName,
                                     String amount) {

        String msg = String.format(
                "Hi %s, your service at %s is completed.\nAmount Paid: ₹%s\nPlease leave a review!",
                customerName, shopName, amount);

        send(phone, msg);
    }

    // ─────────────────────────────────────────
    // OTP / Security
    // ─────────────────────────────────────────

    @Async
    public void sendOtp(String phone, String otp, String expiryMinutes) {

        String msg = String.format(
                "Your bookurbarber login OTP is %s. It will expire in %s minutes.",
                otp, expiryMinutes);

        send(phone, msg);
    }

    @Async
    public void sendPasswordReset(String phone,
                                  String name,
                                  String resetLink,
                                  String expiryHours) {

        String msg = String.format(
                "Hi %s,\nReset your password here:\n%s\nLink expires in %s hours.",
                name, resetLink, expiryHours);

        send(phone, msg);
    }

    @Async
    public void sendRescheduleRequestToCustomer(String phone,
                                                String customerName,
                                                String shopName,
                                                String oldTime,
                                                String newDate,
                                                String newTime,
                                                String reason) {

        String msg = String.format(
                "Hi %s,\n%s requested to reschedule your appointment.\n" +
                        "Old time: %s\nNew time: %s %s\nReason: %s",
                customerName,
                shopName,
                oldTime,
                newDate,
                newTime,
                reason
        );

        send(phone, msg);
    }
    @Async
    public void sendRescheduleResponseToBarber(String barberPhone,
                                               String shopName,
                                               String customerName,
                                               String newTime,
                                               String status) {

        String msg = String.format(
                "%s responded to the reschedule request.\n" +
                        "Customer: %s\nNew time: %s\nStatus: %s",
                shopName,
                customerName,
                newTime,
                status
        );

        send(barberPhone, msg);
    }
    // ─────────────────────────────────────────
    // Core send method
    // ─────────────────────────────────────────

    private void send(String phone, String message) {

        if (!enabled) {
            log.info("[WA-MOCK] to=+91{} message={}", phone, message);
            return;
        }

        try {

            String normalized = "+91" + phone
                    .replaceAll("^(\\+91|91|0)", "")
                    .replaceAll("\\D", "");

            Message twilioMsg = Message.creator(
                    new PhoneNumber("whatsapp:" + normalized),
                    new PhoneNumber("whatsapp:" + sandboxNumber),
                    message
            ).create();

            log.info("[WA] Sent to {} sid={}", normalized, twilioMsg.getSid());

        } catch (Exception e) {

            log.error("[WA] Failed to send message to {} error={}",
                    phone, e.getMessage());
        }
    }
}