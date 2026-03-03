package com.trimly.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SlotBlockRequest {

    @NotNull(message = "Date is required")
    LocalDate blockDate;

    @NotNull(message = "Slot time is required")
    LocalTime slotTime;

    String reason;

    /** Null = block for all employees (shop-wide). Set = block only this employee. */
    Long employeeId;
}
