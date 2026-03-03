package com.trimly.controller;

import com.trimly.dto.*;
import com.trimly.service.ShopService;
import com.trimly.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopPublicController {

    private final ShopService    shopService;
    private final EmployeeService empService;

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<ShopResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String area) {
        return ResponseEntity.ok(ApiResponse.ok(shopService.getPublicShops(q, city, area)));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<ShopResponse>> byId(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(shopService.getPublicShopById(id)));
    }

    @GetMapping("/public/slug/{slug}")
    public ResponseEntity<ApiResponse<ShopResponse>> bySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(shopService.getPublicShopBySlug(slug)));
    }

    /** Customer booking flow — list active employees for a shop */
    @GetMapping("/{id}/employees")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> employees(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(empService.getPublicEmployees(id)));
    }
}
