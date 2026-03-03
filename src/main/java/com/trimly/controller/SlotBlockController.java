package com.trimly.controller;

import com.trimly.dto.*;
import com.trimly.entity.User;
import com.trimly.service.SlotBlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/barber/slots")
@RequiredArgsConstructor
public class SlotBlockController {

    private final SlotBlockService blockService;

    /** All blocked slots for a date (owner view) */
    @GetMapping("/blocked")
    public ResponseEntity<ApiResponse<List<SlotBlockResponse>>> blocked(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(blockService.getBlockedSlots(user.getId(), date)));
    }

    /** Block a single slot */
    @PostMapping("/block")
    public ResponseEntity<ApiResponse<SlotBlockResponse>> block(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SlotBlockRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Slot blocked", blockService.blockSlot(user.getId(), req)));
    }

    /** Block a range of slots */
    @PostMapping("/block-range")
    public ResponseEntity<ApiResponse<List<SlotBlockResponse>>> blockRange(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody SlotBlockRangeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Slots blocked", blockService.blockRange(user.getId(), req)));
    }

    /**
     * Unblock a single slot.
     * employeeId = null → unblock shop-wide block
     * employeeId = <id> → unblock only for that employee
     */
    @DeleteMapping("/unblock")
    public ResponseEntity<ApiResponse<Void>> unblock(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam(required = false) Long employeeId) {
        blockService.unblockSlot(user.getId(), date, time, employeeId);
        return ResponseEntity.ok(ApiResponse.ok("Slot unblocked", null));
    }

    /** Unblock all slots for a specific employee on a date */
    @DeleteMapping("/unblock-all")
    public ResponseEntity<ApiResponse<Void>> unblockAll(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long employeeId) {
        blockService.unblockAllForEmployee(user.getId(), date, employeeId);
        return ResponseEntity.ok(ApiResponse.ok("All slots unblocked for employee", null));
    }
}
