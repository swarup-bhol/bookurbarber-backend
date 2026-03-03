package com.trimly.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlotBlockResponse {
    Long id;
    LocalDate blockDate;
    LocalTime slotTime;
    String reason;
    String blockedBy;
    Long employeeId;
    String employeeName;
    String employeeAvatar;
}
