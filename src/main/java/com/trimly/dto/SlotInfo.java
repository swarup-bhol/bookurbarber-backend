package com.trimly.dto;

import lombok.*;
import java.time.LocalTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlotInfo {
    LocalTime time;
    String label;
    boolean taken;
    boolean available;
    boolean blockedByOwner;
    int seatsTotal;
    int seatsUsed;
    int seatsLeft;
    List<EmployeeSlotStatus> employeeStatuses;
}
