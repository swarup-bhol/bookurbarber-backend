package com.trimly.dto;
import lombok.*; import java.math.BigDecimal;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardStats {
    Long totalBookings; Long pendingBookings; Long confirmedBookings; Long completedBookings;
    BigDecimal totalRevenue; BigDecimal totalCommission; BigDecimal barberEarnings;
    Long totalShops; Long activeShops; Long pendingShops; Long totalCustomers;
}
