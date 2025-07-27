package com.tev.riderapp.controller;

import com.tev.riderapp.dto.RideRequestDto;
import com.tev.riderapp.dto.RideResponseDto;
import com.tev.riderapp.mapper.RideMapper;
import com.tev.riderapp.model.Passenger;
import com.tev.riderapp.model.Ride;
import com.tev.riderapp.service.RideManagementSystem;
import com.tev.riderapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {
    private final RideManagementSystem rideService;
    private final UserService userService;
    private final RideMapper rideMapper;

    @PostMapping("/request")
    public ResponseEntity<RideResponseDto> requestRide(@RequestBody RideRequestDto request) {
        Passenger passenger = userService.getPassenger(request.getPassengerId());
        Ride ride = rideService.requestRideWithPostcode(
            passenger, 
            request.getPickupAddress(), 
            request.getPickupPostcode(),
            request.getDropoffAddress(), 
            request.getDropoffPostcode(), 
            request.getRideType(),
            request.getPaymentMethod()
        );
        return ResponseEntity.ok(rideMapper.toDto(ride));
    }

    @PutMapping("/{rideId}/accept")
    public ResponseEntity<Void> acceptRide(@PathVariable Long rideId, @RequestParam Long driverId) {
        rideService.acceptRide(rideId, driverId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{rideId}/start")
    public ResponseEntity<Void> startRide(@PathVariable Long rideId) {
        rideService.startRide(rideId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{rideId}/complete")
    public ResponseEntity<Void> completeRide(@PathVariable Long rideId) {
        rideService.completeRide(rideId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{rideId}/cancel")
    public ResponseEntity<Void> cancelRide(@PathVariable Long rideId) {
        rideService.cancelRide(rideId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{rideId}/cancel-by-passenger")
    public ResponseEntity<Void> cancelRideByPassenger(@PathVariable Long rideId, @RequestParam Long passengerId) {
        rideService.cancelRideByPassenger(rideId, passengerId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{rideId}")
    public ResponseEntity<RideResponseDto> getRide(@PathVariable Long rideId) {
        Ride ride = rideService.getRide(rideId);
        return ResponseEntity.ok(rideMapper.toDto(ride));
    }

    @GetMapping("/passenger/{passengerId}")
    public ResponseEntity<List<RideResponseDto>> getPassengerRides(@PathVariable Long passengerId) {
        List<Ride> rides = rideService.getPassengerRides(passengerId);
        List<RideResponseDto> dtos = rides.stream()
                .map(rideMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<List<RideResponseDto>> getDriverRides(@PathVariable Long driverId) {
        List<Ride> rides = rideService.getDriverRides(driverId);
        List<RideResponseDto> dtos = rides.stream()
                .map(rideMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/active")
    public ResponseEntity<List<RideResponseDto>> getActiveRides() {
        List<Ride> rides = rideService.getActiveRides();
        List<RideResponseDto> dtos = rides.stream()
                .map(rideMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{rideId}/rate")
    public ResponseEntity<Void> rateRide(@PathVariable Long rideId, @RequestParam int rating) {
        rideService.rateRide(rideId, rating);
        return ResponseEntity.ok().build();
    }
}