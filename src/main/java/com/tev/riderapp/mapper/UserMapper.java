package com.tev.riderapp.mapper;

import com.tev.riderapp.dto.*;
import com.tev.riderapp.model.*;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    public Passenger toEntity(PassengerRegistrationDto dto) {
        Passenger passenger = new Passenger();
        passenger.setFirstName(dto.getFirstName());
        passenger.setLastName(dto.getLastName());
        passenger.setEmail(dto.getEmail());
        passenger.setPhone(dto.getPhone());
        passenger.setPassword(dto.getPassword());
        passenger.setWalletBalance(dto.getWalletBalance());
        return passenger;
    }
    
    public Driver toEntity(DriverRegistrationDto dto) {
        Driver driver = new Driver();
        driver.setFirstName(dto.getFirstName());
        driver.setLastName(dto.getLastName());
        driver.setEmail(dto.getEmail());
        driver.setPhone(dto.getPhone());
        driver.setPassword(dto.getPassword());
        driver.setVehicleNumber(dto.getVehicleNumber());
        driver.setVehicleType(dto.getVehicleType());
        driver.setCurrentLocation(dto.getCurrentLocation());
        return driver;
    }
    
    public UserResponseDto toDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        return dto;
    }
}