package com.trimly.dto;
import jakarta.validation.constraints.*; import lombok.Data; import java.math.BigDecimal;
@Data public class BarberRegisterRequest {
    @NotBlank @Size(min=2,max=100) String fullName;
    @NotBlank @Email String email;
    @NotBlank @Size(min=6) String password;
    @NotBlank @Pattern(regexp="^[6-9]\\d{9}$") String phone;
    @NotBlank @Size(min=2,max=150) String shopName;
    @NotBlank String location;
    @NotBlank String city;
    @NotBlank String area;
    BigDecimal latitude; BigDecimal longitude;
}
