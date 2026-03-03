package com.trimly.dto;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class VerifyOtpRequest {
    @NotBlank @Pattern(regexp="^[6-9]\\d{9}$") String phone;
    @NotBlank @Size(min=6,max=6,message="OTP must be 6 digits") String otp;
    String fullName;
}
