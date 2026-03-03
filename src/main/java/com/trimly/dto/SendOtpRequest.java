package com.trimly.dto;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class SendOtpRequest {
    @NotBlank(message="Phone is required")
    @Pattern(regexp="^[6-9]\\d{9}$", message="Enter a valid 10-digit Indian mobile number")
    String phone;
}
