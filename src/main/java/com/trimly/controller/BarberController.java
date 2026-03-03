package com.trimly.controller;

import com.trimly.dto.*;
import com.trimly.entity.User;
import com.trimly.enums.BookingStatus;
import com.trimly.service.BookingService;
import com.trimly.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/barber")
@RequiredArgsConstructor
public class BarberController {

    private final ShopService    shopService;
    private final BookingService bookingService;

    // ── Shop ─────────────────────────────────────────────────────────────

    @GetMapping("/shop")
    public ResponseEntity<ApiResponse<ShopResponse>> myShop(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(shopService.getBarberShop(user.getId())));
    }

    @PatchMapping("/shop")
    public ResponseEntity<ApiResponse<ShopResponse>> updateShop(
            @AuthenticationPrincipal User user,
            @RequestBody ShopUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Shop updated", shopService.updateShop(user.getId(), req)));
    }

    // ── Services ─────────────────────────────────────────────────────────

    @PostMapping("/services")
    public ResponseEntity<ApiResponse<ServiceResponse>> addService(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ServiceRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Service added", shopService.addService(user.getId(), req)));
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> updateService(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Service updated", shopService.updateService(user.getId(), id, req)));
    }

    @PatchMapping("/services/{id}/toggle")
    public ResponseEntity<ApiResponse<ServiceResponse>> toggleService(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(shopService.toggleService(user.getId(), id)));
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteService(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        shopService.deleteService(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Service deleted", null));
    }

    // ── Bookings ─────────────────────────────────────────────────────────

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<?>> bookings(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) BookingStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getBarberBookings(user.getId(), status)));
    }

    @GetMapping("/bookings/stats")
    public ResponseEntity<ApiResponse<DashboardStats>> stats(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getBarberStats(user.getId())));
    }

    @PostMapping("/bookings/{id}/accept")
    public ResponseEntity<ApiResponse<BookingResponse>> accept(
            @AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Booking accepted — customer notified via WhatsApp",
            bookingService.accept(user.getId(), id)));
    }

    @PostMapping("/bookings/{id}/reject")
    public ResponseEntity<ApiResponse<BookingResponse>> reject(
            @AuthenticationPrincipal User user, @PathVariable Long id,
            @RequestBody(required = false) BookingActionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Booking rejected — customer notified",
            bookingService.reject(user.getId(), id, req != null ? req : new BookingActionRequest())));
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(
            @AuthenticationPrincipal User user, @PathVariable Long id,
            @RequestBody(required = false) BookingActionRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled — customer notified",
            bookingService.cancelByBarber(user.getId(), id, req != null ? req : new BookingActionRequest())));
    }

    @PostMapping("/bookings/{id}/complete")
    public ResponseEntity<ApiResponse<BookingResponse>> complete(
            @AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Booking completed! Customer asked to rate.",
            bookingService.complete(user.getId(), id)));
    }

    @PostMapping("/bookings/{id}/reschedule")
    public ResponseEntity<ApiResponse<BookingResponse>> reschedule(
            @AuthenticationPrincipal User user, @PathVariable Long id,
            @Valid @RequestBody RescheduleRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
            "Reschedule request sent to customer via WhatsApp",
            bookingService.requestReschedule(user.getId(), id, req)));
    }
}
