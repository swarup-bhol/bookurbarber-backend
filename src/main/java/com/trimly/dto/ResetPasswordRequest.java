package com.trimly.dto;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class ResetPasswordRequest {
    @NotBlank String token;
    @NotBlank @Size(min=6) String newPassword;
}
