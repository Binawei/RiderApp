package com.tev.riderapp.dto;

import com.tev.riderapp.model.Location;
import com.tev.riderapp.model.Ride;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RideResponseDto {
    private Long id;
    private String passengerName;
    private String driverName;
    private Location pickupLocation;
    private Location dropoffLocation;
    private LocalDateTime requestTime;
    private Ride.RideStatus status;
    private Ride.RideType rideType;
    private double fare;
    private double distance;
    private double surgeMultiplier;
}