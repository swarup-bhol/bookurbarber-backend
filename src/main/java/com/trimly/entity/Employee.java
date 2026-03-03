package com.trimly.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "employees", indexes = {
    @Index(name = "idx_emp_shop",   columnList = "shop_id"),
    @Index(name = "idx_emp_active", columnList = "shop_id,active")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String role;

    @Column(length = 10)
    @Builder.Default
    private String avatar = "💈";

    @Column(length = 20)
    private String phone;

    @Column(length = 300)
    private String bio;

    /** Comma-separated e.g. "Fade,Beard,Color" */
    @Column(length = 300)
    private String specialties;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private int displayOrder = 0;

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
    private BigDecimal totalEarnings = BigDecimal.ZERO;
}
