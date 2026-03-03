package com.trimly.controller;

import com.trimly.dto.*;
import com.trimly.entity.User;
import com.trimly.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/barber/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService empService;

    /** List all employees (owner view — includes inactive) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> list(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(empService.getMyEmployees(user.getId())));
    }

    /** Add a new employee */
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> add(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody EmployeeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Employee added", empService.addEmployee(user.getId(), req)));
    }

    /** Update employee details */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody EmployeeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Employee updated", empService.updateEmployee(user.getId(), id, req)));
    }

    /** Toggle employee on/off duty */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<EmployeeResponse>> toggle(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(empService.toggleActive(user.getId(), id)));
    }

    /** Delete an employee */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        empService.deleteEmployee(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Employee deleted", null));
    }

    /**
     * End-of-day / date report.
     * date defaults to today if omitted.
     */
    @GetMapping("/report")
    public ResponseEntity<ApiResponse<List<EmployeeStatsResponse>>> report(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(empService.getDailyReport(user.getId(), date)));
    }
}
