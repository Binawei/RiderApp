package com.tev.riderapp.dto;

import lombok.Data;

@Data
public class ResetPasswordRequestDto {
    private String email;
    private String userType;
    private String resetToken;
    private String newPassword;
}