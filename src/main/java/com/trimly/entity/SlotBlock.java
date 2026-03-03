package com.trimly.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "slot_blocks", indexes = {
    @Index(name = "idx_sb_shop_date", columnList = "shop_id,block_date"),
    @Index(name = "idx_sb_employee",  columnList = "employee_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SlotBlock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    /**
     * If null  → shop-wide block (all employees).
     * If set   → only this employee's slot is blocked.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "block_date", nullable = false)
    private LocalDate blockDate;

    @Column(nullable = false)
    private LocalTime slotTime;

    @Column(length = 200)
    private String reason;

    @Column(length = 100)
    @Builder.Default
    private String blockedBy = "OWNER";
}
