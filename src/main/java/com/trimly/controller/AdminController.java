package com.trimly.controller;

import com.trimly.dto.*;
import com.trimly.enums.BookingStatus;
import com.trimly.enums.ShopStatus;
import com.trimly.service.BookingService;
import com.trimly.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ShopService    shopService;
    private final BookingService bookingService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStats>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getAdminStats()));
    }

    @GetMapping("/shops")
    public ResponseEntity<ApiResponse<?>> shops() {
        return ResponseEntity.ok(ApiResponse.ok(shopService.getAllAdmin()));
    }

    @PostMapping("/shops/{id}/approve")
    public ResponseEntity<ApiResponse<ShopResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Shop approved", shopService.setStatus(id, ShopStatus.ACTIVE)));
    }

    @PostMapping("/shops/{id}/disable")
    public ResponseEntity<ApiResponse<ShopResponse>> disable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Shop disabled", shopService.setStatus(id, ShopStatus.DISABLED)));
    }

    @PostMapping("/shops/{id}/enable")
    public ResponseEntity<ApiResponse<ShopResponse>> enable(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Shop re-enabled", shopService.setStatus(id, ShopStatus.ACTIVE)));
    }

    @PatchMapping("/shops/{id}/commission")
    public ResponseEntity<ApiResponse<ShopResponse>> commission(
            @PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        return ResponseEntity.ok(ApiResponse.ok("Commission updated",
            shopService.updateCommission(id, body.get("percent"))));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<?>> bookings(@RequestParam(required = false) BookingStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getAllAdmin(status)));
    }
}
