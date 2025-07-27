package com.tev.riderapp.dto;

import lombok.Data;

@Data
public class PassengerRegistrationDto {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;
    private double walletBalance;
}