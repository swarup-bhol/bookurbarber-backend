package com.trimly.entity;

import com.trimly.enums.BookingStatus;
import com.trimly.enums.RescheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_bk_shop",     columnList = "shop_id"),
    @Index(name = "idx_bk_customer", columnList = "customer_id"),
    @Index(name = "idx_bk_employee", columnList = "employee_id"),
    @Index(name = "idx_bk_status",   columnList = "status"),
    @Index(name = "idx_bk_date",     columnList = "booking_date"),
    @Index(name = "idx_bk_slot",     columnList = "shop_id,booking_date,slot_time")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /** Optional — customer can prefer a specific employee */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    /** Snapshot of employee name at booking time (kept even if employee is deleted) */
    @Column(length = 100)
    private String employeeSnapshot;

    /** Separate rating for the employee (independent from shop rating) */
    @Column
    private Integer employeeRating;

    @Column(nullable = false, length = 500)
    private String servicesSnapshot;

    @Column(length = 500)
    private String serviceIds;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(name = "slot_time", nullable = false)
    private LocalTime slotTime;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    @Builder.Default
    private int seats = 1;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal barberEarning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(length = 500)
    private String cancelReason;

    @Column
    private Integer rating;

    @Column(length = 1000)
    private String review;

    @Column
    private LocalDate rescheduleDate;

    @Column
    private LocalTime rescheduleTime;

    @Column(length = 500)
    private String rescheduleReason;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RescheduleStatus rescheduleStatus;

    @Column(length = 100)
    private String waMsgId;
}
