package com.trimly.controller;

import com.trimly.dto.*;
import com.trimly.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class SlotController {

    private final ShopService shopService;

    @GetMapping("/{id}/slots")
    public ResponseEntity<ApiResponse<SlotAvailabilityResponse>> slots(
            @PathVariable Long id,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Long employeeId) {
        LocalDate d = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(shopService.getSlots(id, d, employeeId)));
    }
}
