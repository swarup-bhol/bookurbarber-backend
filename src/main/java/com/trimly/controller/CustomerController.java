package com.trimly.controller;

import com.trimly.dto.*;
import com.trimly.entity.User;
import com.trimly.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final BookingService bookingService;

    @PostMapping("/bookings")
    public ResponseEntity<ApiResponse<BookingResponse>> book(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BookingRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Booking submitted! Barber will confirm shortly.",
            bookingService.create(user.getId(), req)));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<?>> myBookings(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getCustomerBookings(user.getId())));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(
            @AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled",
            bookingService.cancelByCustomer(user.getId(), id)));
    }

    @PostMapping("/bookings/{id}/rate")
    public ResponseEntity<ApiResponse<BookingResponse>> rate(
            @AuthenticationPrincipal User user, @PathVariable Long id,
            @Valid @RequestBody RatingRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Thanks for your review!",
            bookingService.rate(user.getId(), id, req)));
    }

    @PostMapping("/bookings/{id}/reschedule/respond")
    public ResponseEntity<ApiResponse<BookingResponse>> respondReschedule(
            @AuthenticationPrincipal User user, @PathVariable Long id,
            @RequestBody RescheduleResponseRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
            req.isAccept() ? "Reschedule accepted ✅" : "Declined — original slot kept",
            bookingService.respondToReschedule(user.getId(), id, req)));
    }

    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<UserInfo>> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(
            bookingService.updateCustomerProfile(user.getId(), body.get("fullName"))));
    }
}
