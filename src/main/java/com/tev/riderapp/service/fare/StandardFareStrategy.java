package com.tev.riderapp.service.fare;

import com.tev.riderapp.model.Ride;
import org.springframework.stereotype.Component;

@Component
public class StandardFareStrategy implements FareCalculationStrategy {
    private static final double BASE_FARE = 5.0;
    private static final double RATE_PER_KM = 2.0;
    
    @Override
    public double calculateFare(Ride ride) {
        double distanceFare = ride.getDistance() * RATE_PER_KM;
        return (BASE_FARE + distanceFare) * ride.getSurgeMultiplier();
    }
}