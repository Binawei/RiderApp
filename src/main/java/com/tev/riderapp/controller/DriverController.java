package com.tev.riderapp.controller;

import com.tev.riderapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/drivers")
public class DriverController {

    private UserService userService;
    
    @GetMapping("/{driverId}/earnings")
    public ResponseEntity<Double> getDriverEarnings(@PathVariable Long driverId) {
        try {
            double earnings = userService.getDriverEarnings(driverId);
            return ResponseEntity.ok(earnings);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{driverId}/rating")
    public ResponseEntity<Double> getDriverRating(@PathVariable Long driverId) {
        try {
            double rating = userService.getDriverRating(driverId);
            return ResponseEntity.ok(rating);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}