package com.trimly.controller;

import com.trimly.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/legal")
public class LegalController {

    @GetMapping("/terms")
    public ResponseEntity<ApiResponse<?>> terms() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "title", "Terms & Conditions",
            "lastUpdated", "01 March 2026",
            "company", "Trimly Technologies Pvt. Ltd.",
            "address", "Bangalore, Karnataka, India",
            "sections", List.of(
                s("1. Acceptance of Terms",
                    "By accessing Trimly you agree to these Terms. If you disagree, please stop using the platform."),
                s("2. About Trimly",
                    "Trimly is a technology platform connecting customers with registered barbershops. We facilitate bookings but are not a barbershop. We are not responsible for service quality provided by individual shops."),
                s("3. Booking Policy",
                    "A booking request is not guaranteed until confirmed by the barber. Customers must arrive within 10 minutes of their slot. Barbers may reject bookings at their discretion. Multiple no-shows may result in account restriction."),
                s("4. Cancellation Policy",
                    "Customers may cancel pending or confirmed bookings through the app. Cancellations within 2 hours of the slot may affect your profile. Barbers may cancel confirmed bookings in exceptional circumstances with a reason provided."),
                s("5. Platform Commission",
                    "Trimly charges 10% commission on completed bookings, shown transparently in the barber dashboard. No commission is charged on rejected or cancelled bookings."),
                s("6. Ratings & Reviews",
                    "Customers may rate and review barbers after a completed booking. Reviews must be honest. Trimly may remove reviews that are abusive or false. Barbers may not solicit fake reviews."),
                s("7. Prohibited Uses",
                    "You may not: make fraudulent bookings, harass users, circumvent platform fees, post false reviews, or reverse-engineer the platform."),
                s("8. Limitation of Liability",
                    "Trimly's maximum liability is limited to the platform fee collected in the 3 months preceding any claim."),
                s("9. Governing Law",
                    "These terms are governed by Indian law. Disputes are subject to courts in Bangalore, Karnataka."),
                s("10. Contact", "legal@trimly.app · support@trimly.app")
            )
        )));
    }

    @GetMapping("/privacy")
    public ResponseEntity<ApiResponse<?>> privacy() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "title", "Privacy Policy",
            "lastUpdated", "01 March 2026",
            "company", "Trimly Technologies Pvt. Ltd.",
            "sections", List.of(
                s("1. Data Controller", "Trimly Technologies Pvt. Ltd. · privacy@trimly.app"),
                s("2. Data We Collect",
                    "Customers: mobile number (mandatory), name, booking history, location (only when Near Me is used with permission). Barbers: name, email, phone, shop details, revenue analytics. All: device type, session timestamps, WhatsApp delivery status."),
                s("3. How We Use It",
                    "Authenticate via WhatsApp OTP · Send booking notifications · Show nearby shops · Platform analytics to improve service. We never sell your data."),
                s("4. WhatsApp Consent",
                    "By providing your number you consent to WhatsApp messages for OTPs, booking confirmations and service updates. Opt out: support@trimly.app (note: opting out of OTPs prevents login)."),
                s("5. Location",
                    "Accessed only when you tap Near Me. Not continuously tracked. Not stored on our servers."),
                s("6. Data Sharing",
                    "Barbers (your name + phone for your booking) · Meta/WhatsApp (delivery) · Cloud hosting (encrypted). Never with advertisers or data brokers."),
                s("7. Retention",
                    "Active accounts: retained while active · Booking history: 5 years · Inactive (>24 months): anonymised · OTPs: deleted immediately after use."),
                s("8. Security",
                    "HTTPS/TLS · BCrypt password hashing · OTPs hashed before storage · JWT tokens · DB access restricted to app servers."),
                s("9. Your Rights (DPDP Act 2023)",
                    "Access · Correct · Delete · Withdraw consent. Email privacy@trimly.app — response within 72 hours."),
                s("10. Contact", "privacy@trimly.app")
            )
        )));
    }

    @GetMapping("/refund")
    public ResponseEntity<ApiResponse<?>> refund() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "title", "Refund & Cancellation Policy",
            "lastUpdated", "01 March 2026",
            "company", "Trimly Technologies Pvt. Ltd.",
            "sections", List.of(
                s("1. Payment Model",
                    "Customers pay barbers directly at the shop. Trimly does not collect payment at booking time — most refund scenarios do not apply."),
                s("2. Commission",
                    "10% platform commission charged only on Completed bookings. Rejected or cancelled bookings incur zero commission."),
                s("3. Barber Subscriptions",
                    "Starter ₹499/mo · Pro ₹999/mo billed in advance. Cancellation within 3 days of billing = full refund. After 3 days = no refund for current month. Refunds processed within 7 working days."),
                s("4. Disputes",
                    "If service was not rendered despite a confirmed booking: App → My Bookings → Report Issue, or email support@trimly.app. Response within 48 hours."),
                s("5. Contact",
                    "Billing: billing@trimly.app · Support: support@trimly.app")
            )
        )));
    }

    private Map<String, String> s(String heading, String body) {
        return Map.of("heading", heading, "body", body);
    }
}
