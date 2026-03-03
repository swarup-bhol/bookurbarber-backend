package com.trimly.entity;

import com.trimly.enums.PlanType;
import com.trimly.enums.ShopStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shops", indexes = {
    @Index(name = "idx_shop_status", columnList = "status"),
    @Index(name = "idx_shop_slug",   columnList = "slug",   unique = true),
    @Index(name = "idx_shop_city",   columnList = "city"),
    @Index(name = "idx_shop_area",   columnList = "area")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Shop extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String shopName;

    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    @Column(length = 200)
    private String location;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String area;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 1000)
    private String bio;

    @Column(length = 10)
    @Builder.Default
    private String emoji = "✂️";

    @Column(length = 20)
    private String phone;

    @Column(length = 10)
    private String color1;

    @Column(length = 10)
    private String color2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ShopStatus status = ShopStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PlanType plan = PlanType.STARTER;

    @Column(nullable = false)
    @Builder.Default
    private boolean isOpen = false;

    @Column(nullable = false)
    @Builder.Default
    private int seats = 2;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal commissionPercent = BigDecimal.TEN;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal subscriptionFee = new BigDecimal("499");

    @Column(length = 100)
    @Builder.Default
    private String workDays = "Mon,Tue,Wed,Thu,Fri,Sat";

    @Column
    @Builder.Default
    private LocalTime openTime = LocalTime.of(9, 0);

    @Column
    @Builder.Default
    private LocalTime closeTime = LocalTime.of(20, 0);

    @Column(nullable = false)
    @Builder.Default
    private int slotDurationMinutes = 30;

    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private int totalReviews = 0;

    @Column(nullable = false)
    @Builder.Default
    private int totalBookings = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal monthlyRevenue = BigDecimal.ZERO;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BarberService> services = new ArrayList<>();

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Booking> bookings = new ArrayList<>();

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<Employee> employees = new ArrayList<>();

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<SlotBlock> slotBlocks = new ArrayList<>();
}
