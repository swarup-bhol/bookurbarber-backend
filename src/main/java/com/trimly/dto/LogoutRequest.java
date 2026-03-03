package com.trimly.dto;

import lombok.Data;

@Data
public class LogoutRequest {
    String refreshToken; // if provided, only this device session is removed
    boolean allDevices;  // if true, logout from ALL devices
}
