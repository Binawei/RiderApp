package com.tev.riderapp.controller;

import com.tev.riderapp.dto.*;
import com.tev.riderapp.dto.ForgotPasswordRequestDto;
import com.tev.riderapp.dto.ResetPasswordRequestDto;
import com.tev.riderapp.mapper.UserMapper;
import com.tev.riderapp.model.Driver;
import com.tev.riderapp.model.Location;
import com.tev.riderapp.model.Passenger;

import com.tev.riderapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/passengers/register")
    public ResponseEntity<UserResponseDto> registerPassenger(@RequestBody PassengerRegistrationDto dto) {
        log.info("Registering passenger with email: {}", dto.getEmail());
        Passenger passenger = userMapper.toEntity(dto);
        Passenger saved = userService.registerPassenger(passenger);
        return ResponseEntity.ok(userMapper.toDto(saved));
    }

    @PostMapping("/drivers/register")
    public ResponseEntity<UserResponseDto> registerDriver(@RequestBody DriverRegistrationDto dto) {
        Driver driver = userMapper.toEntity(dto);
        Driver saved = userService.registerDriver(driver);
        return ResponseEntity.ok(userMapper.toDto(saved));
    }

    @PostMapping("/passengers/login")
    public ResponseEntity<UserResponseDto> loginPassenger(@RequestBody LoginRequestDto dto) {
        Passenger passenger = userService.loginPassenger(dto.getEmail(), dto.getPassword());
        return ResponseEntity.ok(userMapper.toDto(passenger));
    }

    @PostMapping("/drivers/login")
    public ResponseEntity<UserResponseDto> loginDriver(@RequestBody LoginRequestDto dto) {
        Driver driver = userService.loginDriver(dto.getEmail(), dto.getPassword());
        return ResponseEntity.ok(userMapper.toDto(driver));
    }


    @GetMapping("/passengers/{id}")
    public ResponseEntity<UserResponseDto> getPassenger(@PathVariable Long id) {
        Passenger passenger = userService.getPassenger(id);
        return ResponseEntity.ok(userMapper.toDto(passenger));
    }

    @GetMapping("/drivers/{id}")
    public ResponseEntity<UserResponseDto> getDriver(@PathVariable Long id) {
        Driver driver = userService.getDriver(id);
        return ResponseEntity.ok(userMapper.toDto(driver));
    }

    @PutMapping("/drivers/{id}/location")
    public ResponseEntity<Void> updateDriverLocation(@PathVariable Long id, @RequestBody Location location) {
        userService.updateDriverLocation(id, location);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/drivers/{id}/availability")
    public ResponseEntity<Void> updateDriverAvailability(@PathVariable Long id, @RequestParam boolean available) {
        userService.updateDriverAvailability(id, available);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/drivers/available")
    public ResponseEntity<List<UserResponseDto>> getAvailableDrivers() {
        List<Driver> drivers = userService.getAvailableDrivers();
        List<UserResponseDto> dtos = drivers.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequestDto request) {
        try {
            userService.sendPasswordResetToken(request.getEmail(), request.getUserType());
            return ResponseEntity.ok("Password reset link sent to your email");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequestDto request) {
        try {
            userService.resetPassword(request.getEmail(), request.getUserType(), 
                                    request.getResetToken(), request.getNewPassword());
            return ResponseEntity.ok("Password reset successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @PostMapping("/passengers/{passengerId}/fund-wallet")
    public ResponseEntity<String> fundWallet(@PathVariable Long passengerId, @RequestParam double amount) {
        try {
            userService.fundWallet(passengerId, amount);
            return ResponseEntity.ok("Wallet funded successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/passengers/{passengerId}/wallet-balance")
    public ResponseEntity<Double> getWalletBalance(@PathVariable Long passengerId) {
        try {
            double balance = userService.getWalletBalance(passengerId);
            return ResponseEntity.ok(balance);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}