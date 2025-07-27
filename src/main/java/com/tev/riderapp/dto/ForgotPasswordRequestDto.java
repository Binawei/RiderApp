package com.tev.riderapp.dto;

import lombok.Data;

@Data
public class ForgotPasswordRequestDto {
    private String email;
    private String userType; // "passenger" or "driver"
}