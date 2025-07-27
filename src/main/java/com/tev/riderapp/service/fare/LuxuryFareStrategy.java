package com.tev.riderapp.service.fare;

import com.tev.riderapp.model.Ride;
import org.springframework.stereotype.Component;

@Component
public class LuxuryFareStrategy implements FareCalculationStrategy {
    private static final double RATE_PER_KM = 0.50;
    
    @Override
    public double calculateFare(Ride ride) {
        double distanceFare = ride.getDistance() * RATE_PER_KM;
        return distanceFare * ride.getSurgeMultiplier();
    }
}