package com.trimly.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SlotBlockRangeRequest {

    @NotNull(message = "Date is required")
    LocalDate blockDate;

    @NotNull(message = "From time is required")
    LocalTime fromTime;

    @NotNull(message = "To time is required")
    LocalTime toTime;

    String reason;

    /** Null = block for all employees. Set = block only this employee. */
    Long employeeId;
}
