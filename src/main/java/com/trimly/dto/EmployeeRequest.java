package com.trimly.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmployeeRequest {

    @NotBlank(message = "Employee name is required")
    String name;

    String role;
    String avatar;
    String phone;
    String bio;

    /** Comma-separated specialties e.g. "Fade,Beard,Color" */
    String specialties;

    Boolean active;
    Integer displayOrder;
}
