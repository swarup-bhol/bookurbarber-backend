package com.trimly.dto;
import com.trimly.enums.ServiceCategory; import jakarta.validation.constraints.*; import lombok.Data;
import java.math.BigDecimal;
@Data public class ServiceRequest {
    @NotBlank @Size(max=150) String serviceName;
    @Size(max=500) String description;
    @NotNull ServiceCategory category;
    @NotNull @DecimalMin("1.00") BigDecimal price;
    @Min(5) @Max(300) int durationMinutes;
    String icon; boolean isCombo;
}
