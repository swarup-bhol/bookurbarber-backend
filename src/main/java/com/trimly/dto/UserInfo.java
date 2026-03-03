package com.trimly.dto;

import com.trimly.enums.Role;
import com.trimly.enums.ShopStatus;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserInfo {
    Long id;
    String fullName;
    String email;
    String phone;
    Role role;
    Long shopId;
    String shopName;
    ShopStatus shopStatus;
}
