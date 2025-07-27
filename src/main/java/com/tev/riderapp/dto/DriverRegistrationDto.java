package com.tev.riderapp.dto;

import com.tev.riderapp.model.Location;
import lombok.Data;

@Data
public class DriverRegistrationDto {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String password;
    private String vehicleNumber;
    private String vehicleType;
    private Location currentLocation;
}