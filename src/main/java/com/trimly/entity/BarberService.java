package com.trimly.entity;

import com.trimly.enums.ServiceCategory;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "shop_services", indexes = {
    @Index(name = "idx_svc_shop",     columnList = "shop_id"),
    @Index(name = "idx_svc_category", columnList = "category"),
    @Index(name = "idx_svc_enabled",  columnList = "enabled")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BarberService extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(nullable = false, length = 150)
    private String serviceName;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ServiceCategory category = ServiceCategory.HAIR;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(length = 10)
    @Builder.Default
    private String icon = "✂️";

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean isCombo = false;
}
