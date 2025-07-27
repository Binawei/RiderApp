package com.tev.riderapp.service.fare;


import com.tev.riderapp.model.Ride;

public interface FareCalculationStrategy {
    double calculateFare(Ride ride);
}