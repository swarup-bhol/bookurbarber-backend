package com.trimly.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class BookingRequest {

    @NotNull
    Long shopId;

    @NotEmpty(message = "Select at least one service")
    List<Long> serviceIds;

    @NotNull
    @FutureOrPresent
    LocalDate bookingDate;

    @NotNull
    LocalTime slotTime;

    @Min(1) @Max(4)
    int seats = 1;

    /** Optional — customer can prefer a specific employee. Null = any available. */
    Long employeeId;
}
