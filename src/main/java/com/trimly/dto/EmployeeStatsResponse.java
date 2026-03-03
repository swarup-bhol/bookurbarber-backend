package com.trimly.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeStatsResponse {
    Long employeeId;
    String employeeName;
    String employeeAvatar;
    String employeeRole;

    // Today
    int bookingsToday;
    int completedToday;
    BigDecimal earningsToday;

    // This month
    int bookingsMonth;
    int completedMonth;
    BigDecimal earningsMonth;

    // All time
    int totalBookings;
    BigDecimal totalEarnings;
    BigDecimal avgRating;
    int totalReviews;

    int blockedSlotsToday;
}
