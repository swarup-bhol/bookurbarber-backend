package com.trimly.dto;
import lombok.Data; import java.math.BigDecimal; import java.time.LocalTime;
@Data public class ShopUpdateRequest {
    String shopName; String location; String city; String area;
    BigDecimal latitude; BigDecimal longitude; String bio; String emoji; String phone;
    String color1; String color2; Boolean isOpen; Integer seats; String workDays;
    LocalTime openTime; LocalTime closeTime; Integer slotDurationMinutes;
}
