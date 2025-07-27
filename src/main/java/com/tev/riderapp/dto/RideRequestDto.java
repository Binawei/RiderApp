package com.tev.riderapp.dto;

import com.tev.riderapp.model.Ride;
import lombok.Data;

@Data
public class RideRequestDto {
    private Long passengerId;
    private String pickupAddress;
    private String pickupPostcode;
    private String dropoffAddress;
    private String dropoffPostcode;
    private Ride.RideType rideType;
    private String paymentMethod; // "WALLET" or "CREDIT_CARD"
}