package com.trimly.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeSlotStatus {
    Long employeeId;
    String employeeName;
    String employeeAvatar;
    boolean available;
    boolean blocked;
    boolean booked;
}
