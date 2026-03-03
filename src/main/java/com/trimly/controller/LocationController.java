package com.trimly.controller;

import com.trimly.dto.*;
import com.trimly.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final ShopService shopService;

    /** Returns all cities + areas that have at least one active shop */
    @GetMapping("/meta")
    public ResponseEntity<ApiResponse<LocationMeta>> meta() {
        return ResponseEntity.ok(ApiResponse.ok(shopService.getLocationMeta()));
    }
}
