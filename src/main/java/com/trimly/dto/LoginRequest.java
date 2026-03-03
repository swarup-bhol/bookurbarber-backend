package com.trimly.dto;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class LoginRequest {
    @NotBlank @Email String email;
    @NotBlank String password;
}
