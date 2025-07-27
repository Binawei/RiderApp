package com.tev.riderapp.service;

import com.tev.riderapp.model.*;
import com.tev.riderapp.observer.RideObserver;
import com.tev.riderapp.observer.PassengerNotifier;
import com.tev.riderapp.observer.DriverNotifier;
import com.tev.riderapp.rideshare.repository.RideRepository;
import com.tev.riderapp.service.fare.FareCalculationStrategy;
import com.tev.riderapp.service.fare.LuxuryFareStrategy;
import com.tev.riderapp.service.fare.PoolFareStrategy;
import com.tev.riderapp.service.fare.StandardFareStrategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RideManagementSystem {
    private List<Driver> availableDrivers = new ArrayList<>();
    private List<Ride> activeRides = new ArrayList<>();
    private List<RideObserver> observers = new ArrayList<>();
    
    @Autowired
    private RideRepository rideRepository;
    
    @Autowired
    private GoogleMapsService googleMapsService;
    

    
    public RideManagementSystem() {
        // Initialize collections
        this.availableDrivers = new ArrayList<>();
        this.activeRides = new ArrayList<>();
        this.observers = new ArrayList<>();
        
        // Add observers for notifications
        addObserver(new PassengerNotifier());
        addObserver(new DriverNotifier());
    }


    public Ride requestRideWithPostcode(Passenger passenger, String pickupAddress, String pickupPostcode, 
                                       String dropoffAddress, String dropoffPostcode, Ride.RideType rideType, String paymentMethod) {
        // Geocode postcodes to get coordinates
        Location pickup = googleMapsService.geocodePostcode(pickupPostcode);
        Location dropoff = googleMapsService.geocodePostcode(dropoffPostcode);

        // Update addresses if provided
        if (pickupAddress != null && !pickupAddress.isEmpty()) {
            pickup.setAddress(pickupAddress);
        }
        if (dropoffAddress != null && !dropoffAddress.isEmpty()) {
            dropoff.setAddress(dropoffAddress);
        }
        
        return requestRide(passenger, pickup, dropoff, rideType, paymentMethod);
    }
    
    public Ride requestRide(Passenger passenger, Location pickup, Location destination, Ride.RideType rideType, String paymentMethod){
            Ride ride = new Ride();
            ride.setPassenger(passenger);
            ride.setPickupLocation(pickup);
            ride.setDropoffLocation(destination);
            ride.setRideType(rideType);
            ride.setPaymentMethod(paymentMethod);
            ride.setStatus(Ride.RideStatus.REQUESTED);
            ride.setRequestTime(LocalDateTime.now());

            // Calculate estimated fare using Google Maps API
            double distance = googleMapsService.calculateDistance(pickup, destination);
            ride.setDistance(distance);
            ride.setSurgeMultiplier(calculateSurgeMultiplier());

            FareCalculationStrategy fareStrategy = getFareStrategy(rideType);
            ride.setFare(fareStrategy.calculateFare(ride));

           Ride rides = rideRepository.save(ride);
           log.info("Ride Cost: " + rides.getFare());

            activeRides.add(rides);
            notifyObservers(rides);
            return ride;
        }

    public void addObserver(RideObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(RideObserver observer) {
        observers.remove(observer);
    }


        public void startRide(Ride ride) {
            ride.setStatus(Ride.RideStatus.PICKED_UP);
            ride.setPickupTime(LocalDateTime.now());
            rideRepository.save(ride);
        }

        public void completeRide (Ride ride){
            ride.setStatus(Ride.RideStatus.COMPLETED);
            ride.setDropoffTime(LocalDateTime.now());

            // Calculate final fare
            FareCalculationStrategy fareStrategy = getFareStrategy(ride.getRideType());
            ride.setFare(fareStrategy.calculateFare(ride));

            // Process payment - transfer from passenger to driver wallet
            processPayment(ride);
            
            // Make driver available again
            Driver driver = ride.getDriver();
            if (driver != null) {
                driver.setAvailable(true);
                availableDrivers.add(driver);
            }
            
            // Save the completed ride
            rideRepository.save(ride);
            
            activeRides.remove(ride);
        }
        
        private void processPayment(Ride ride) {
            Passenger passenger = ride.getPassenger();
            Driver driver = ride.getDriver();
            double fare = ride.getFare();
            String paymentMethod = ride.getPaymentMethod();
            
            if ("WALLET".equals(paymentMethod)) {
                // Check if passenger has sufficient balance
                if (passenger.getWalletBalance() < fare) {
                    throw new RuntimeException("Insufficient wallet balance");
                }
                
                // Deduct from passenger wallet
                passenger.setWalletBalance(passenger.getWalletBalance() - fare);

                // Create payment record
                Payment payment = new Payment();
                payment.setRide(ride);
                payment.setAmount(fare);
                payment.setPaymentType(PaymentType.WALLET);
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTimestamp(LocalDateTime.now());
                
                System.out.println("Wallet payment processed: £" + String.format("%.2f", fare));
            } else {
                // Credit card payment - will be processed via Stripe on frontend
                // For now, assume payment is successful (actual verification happens on frontend)
                Payment payment = new Payment();
                payment.setRide(ride);
                payment.setAmount(fare);
                payment.setPaymentType(PaymentType.CREDIT_CARD);
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTimestamp(LocalDateTime.now());
                
                System.out.println("Credit card payment processed: £" + String.format("%.2f", fare));
            }
            if (driver != null) {
                driver.setEarnings(driver.getEarnings() + fare);
            }
        }

        private double calculateDistance (Location pickup, Location destination){
            // Simple distance calculation using Haversine formula
            final int R = 6371; // Earth's radius in kilometers

            double lat1 = Math.toRadians(pickup.getLatitude());
            double lon1 = Math.toRadians(pickup.getLongitude());
            double lat2 = Math.toRadians(destination.getLatitude());
            double lon2 = Math.toRadians(destination.getLongitude());

            double dlon = lon2 - lon1;
            double dlat = lat2 - lat1;

            double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

            return R * c;
        }

        private double calculateSurgeMultiplier () {
            // Simple surge pricing based on number of active rides
            if (activeRides.size() > 10) return 2.0;
            if (activeRides.size() > 5) return 1.5;
            return 1.0;
        }

        private Optional<Driver> findNearestDriver (Location pickup){
            return availableDrivers.stream()
                    .min((d1, d2) -> {
                        Location driverLoc1 = new Location();
                        driverLoc1.setLatitude(d1.getCurrentLocation().getLatitude());
                        driverLoc1.setLongitude(d1.getCurrentLocation().getLongitude());
                        
                        Location driverLoc2 = new Location();
                        driverLoc2.setLatitude(d2.getCurrentLocation().getLatitude());
                        driverLoc2.setLongitude(d2.getCurrentLocation().getLongitude());
                        
                        double dist1 = calculateDistance(driverLoc1, pickup);
                        double dist2 = calculateDistance(driverLoc2, pickup);
                        return Double.compare(dist1, dist2);
                    });
        }

        public void addDriver (Driver driver){
            if (driver.isAvailable()) {
                availableDrivers.add(driver);
            }
        }

        public void removeDriver (Driver driver){
            availableDrivers.remove(driver);
        }

        public List<Ride> getActiveRides () {
            // Return all REQUESTED rides from database, not just in-memory list
            return rideRepository.findByStatus(Ride.RideStatus.REQUESTED);
        }


    private void notifyObservers(Ride ride) {
        for (RideObserver observer : observers) {
            observer.update(ride);
        }
    }

    public void acceptRide(Long rideId, Long driverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        // Check if ride is still available
        if (ride.getStatus() != Ride.RideStatus.REQUESTED) {
            throw new RuntimeException("Ride is no longer available");
        }
        
        // Assign driver to ride
        Driver driver = new Driver();
        driver.setId(driverId);
        ride.setDriver(driver);
        ride.setStatus(Ride.RideStatus.ACCEPTED);
        
        rideRepository.save(ride);
        notifyObservers(ride);
    }

    public void startRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        startRide(ride);
        notifyObservers(ride);
    }

    public void completeRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        completeRide(ride);
        notifyObservers(ride);
    }

    public void cancelRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        ride.setStatus(Ride.RideStatus.CANCELLED);
        if (ride.getDriver() != null) {
            ride.getDriver().setAvailable(true);
        }
        rideRepository.save(ride);
        activeRides.remove(ride);
        notifyObservers(ride);
    }
    
    public void cancelRideByPassenger(Long rideId, Long passengerId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        
        // Check if ride belongs to the passenger
        if (!ride.getPassenger().getId().equals(passengerId)) {
            throw new RuntimeException("Unauthorized: Ride does not belong to this passenger");
        }
        
        // Only allow cancellation if ride is still REQUESTED (not accepted)
        if (ride.getStatus() != Ride.RideStatus.REQUESTED) {
            throw new RuntimeException("Cannot cancel ride: Ride has already been accepted");
        }
        
        // Cancel the ride
        ride.setStatus(Ride.RideStatus.CANCELLED);
        
        // Make driver available again if assigned
        if (ride.getDriver() != null) {
            ride.getDriver().setAvailable(true);
            availableDrivers.add(ride.getDriver());
        }
        
        rideRepository.save(ride);
        activeRides.remove(ride);
        notifyObservers(ride);
    }

    public Ride getRide(Long rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
    }

    public List<Ride> getPassengerRides(Long passengerId) {
        // In a real implementation, you'd get passenger from repository
        Passenger passenger = new Passenger();
        passenger.setId(passengerId);
        return rideRepository.findByPassenger(passenger);
    }

    public List<Ride> getDriverRides(Long driverId) {
        // In a real implementation, you'd get driver from repository
        Driver driver = new Driver();
        driver.setId(driverId);
        return rideRepository.findByDriver(driver);
    }

    public void rateRide(Long rideId, int rating) {
        Ride ride = getRide(rideId);
        ride.setRating(rating);
        
        // Update driver's overall rating
        Driver driver = ride.getDriver();
        if (driver != null) {
            updateDriverRating(driver, rating);
        }
        
        rideRepository.save(ride);
        System.out.println("Ride rated: " + rating + " stars");
    }
    
    private void updateDriverRating(Driver driver, int newRating) {
        // Simple average calculation (in production, use more sophisticated method)
        double currentRating = driver.getRating();
        int totalRides = driver.getTotalRides() != null ? driver.getTotalRides() : 0;
        
        double newAverageRating = ((currentRating * totalRides) + newRating) / (totalRides + 1);
        driver.setRating(newAverageRating);
        driver.setTotalRides(totalRides + 1);
    }

    public FareCalculationStrategy getFareStrategy(Ride.RideType rideType) {
        switch (rideType) {
            case LUXURY:
                return new LuxuryFareStrategy();
            case POOL:
                return new PoolFareStrategy();
            default:
                return new StandardFareStrategy();
        }
    }
}