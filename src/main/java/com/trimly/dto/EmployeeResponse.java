package com.trimly.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeResponse {
    Long id;
    Long shopId;
    String name;
    String role;
    String avatar;
    String phone;
    String bio;
    String specialties;
    boolean active;
    int displayOrder;
    BigDecimal avgRating;
    int totalReviews;
    int totalBookings;
    BigDecimal totalEarnings;

    // Live computed fields (populated by EmployeeService.toLiveResponse)
    boolean currentlyBusy;
    String nextAvailableSlot;
}
