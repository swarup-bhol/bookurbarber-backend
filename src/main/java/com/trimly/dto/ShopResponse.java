package com.trimly.dto;

import com.trimly.enums.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ShopResponse {
    Long id;
    String shopName;
    String slug;
    String location;
    String city;
    String area;
    BigDecimal latitude;
    BigDecimal longitude;
    String bio;
    String emoji;
    String phone;
    String color1;
    String color2;
    ShopStatus status;
    PlanType plan;
    boolean isOpen;
    int seats;
    BigDecimal avgRating;
    int totalReviews;
    int totalBookings;
    BigDecimal monthlyRevenue;
    String workDays;
    LocalTime openTime;
    LocalTime closeTime;
    int slotDurationMinutes;
    BigDecimal subscriptionFee;
    BigDecimal commissionPercent;
    Long ownerId;
    String ownerName;
    String ownerEmail;
    LocalDateTime createdAt;
    List<ServiceResponse> services;

    // Employee quick-view (populated in public browse)
    int totalEmployees;
    int freeEmployeesNow;
}
