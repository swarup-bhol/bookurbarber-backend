package com.trimly.dto;
import com.trimly.enums.ServiceCategory; import lombok.*;
import java.math.BigDecimal;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ServiceResponse {
    Long id; String serviceName; String description; ServiceCategory category;
    BigDecimal price; int durationMinutes; String icon; boolean enabled; boolean isCombo;
    BigDecimal platformFee; BigDecimal barberEarning;
}
