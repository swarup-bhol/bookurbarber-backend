package com.trimly.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RatingRequest {

    @NotNull @Min(1) @Max(5)
    Integer rating;

    @Size(max = 1000)
    String review;

    /** Optional — rate the specific employee who served you */
    @Min(1) @Max(5)
    Integer employeeRating;
}
