package com.trimly.dto;
import jakarta.validation.constraints.*; import lombok.Data;
@Data public class ForgotPasswordRequest { @NotBlank @Email String email; }
