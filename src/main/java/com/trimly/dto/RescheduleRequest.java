package com.trimly.dto;
import jakarta.validation.constraints.*; import lombok.Data;
import java.time.LocalDate; import java.time.LocalTime;
@Data public class RescheduleRequest {
    @NotNull LocalDate newDate;
    @NotNull LocalTime newTime;
    @NotBlank @Size(max=500) String reason;
}
