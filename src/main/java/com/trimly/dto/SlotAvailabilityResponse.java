package com.trimly.dto;
import lombok.*; import java.time.LocalDate; import java.util.List;
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlotAvailabilityResponse {
    LocalDate date; List<SlotInfo> slots; int totalSlots; int availableSlots;
}
