package com.tev.riderapp.service;
import com.tev.riderapp.model.Driver;
import com.tev.riderapp.model.Location;
import com.tev.riderapp.model.Passenger;
import com.tev.riderapp.rideshare.repository.DriverRepository;
import com.tev.riderapp.rideshare.repository.PassengerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final PassengerRepository passengerRepository;
    private final DriverRepository driverRepository;

    public Passenger registerPassenger(Passenger passenger) {
        if (passengerRepository.findByEmail(passenger.getEmail()) != null) {
            throw new RuntimeException("Email already registered");
        }
        return passengerRepository.save(passenger);
    }

    public Driver registerDriver(Driver driver) {
        if (driverRepository.findByEmail(driver.getEmail()) != null) {
            throw new RuntimeException("Email already registered");
        }
        
        driver.setAvailable(true);
        driver.setRating(0.0);
        driver.setEarnings(0.0);
        return driverRepository.save(driver);
    }

    public Passenger loginPassenger(String email, String password) {
        Passenger passenger = passengerRepository.findByEmail(email);
        if (passenger != null && passenger.getPassword().equals(password)) {
            return passenger;
        }
        throw new RuntimeException("Invalid credentials");
    }

    public Driver loginDriver(String email, String password) {
        Driver driver = driverRepository.findByEmail(email);
        if (driver != null && driver.getPassword().equals(password)) {
            return driver;
        }
        throw new RuntimeException("Invalid credentials");
    }

    public Passenger getPassenger(Long id) {
        return passengerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));
    }

    public Driver getDriver(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
    }

    public void updateDriverLocation(Long driverId, Location location) {
        Driver driver = getDriver(driverId);
        driver.setCurrentLocation(location);
        driverRepository.save(driver);
    }

    public void updateDriverAvailability(Long driverId, boolean available) {
        Driver driver = getDriver(driverId);
        driver.setAvailable(available);
        driverRepository.save(driver);
    }

    public List<Driver> getAvailableDrivers() {
        return driverRepository.findByAvailableTrue();
    }
    
    public void sendPasswordResetToken(String email, String userType) {
        if (userType.equals("passenger")) {
            Passenger passenger = passengerRepository.findByEmail(email);
            if (passenger == null) {
                throw new RuntimeException("Email not found");
            }
            // Generate reset token (simplified - in production use UUID and expiry)
            String resetToken = "RESET_" + System.currentTimeMillis();
            // In production, store token in database with expiry
            System.out.println("Password reset token for " + email + ": " + resetToken);
            // Send email with reset link (simplified)
            System.out.println("Reset link: http://localhost:3000/reset-password?token=" + resetToken + "&email=" + email + "&userType=" + userType);
        } else {
            Driver driver = driverRepository.findByEmail(email);
            if (driver == null) {
                throw new RuntimeException("Email not found");
            }
            String resetToken = "RESET_" + System.currentTimeMillis();
            System.out.println("Password reset token for " + email + ": " + resetToken);
            System.out.println("Reset link: http://localhost:3000/reset-password?token=" + resetToken + "&email=" + email + "&userType=" + userType);
        }
    }
    
    public void resetPassword(String email, String userType, String resetToken, String newPassword) {
        // In production, validate token from database
        if (!resetToken.startsWith("RESET_")) {
            throw new RuntimeException("Invalid reset token");
        }
        
        if (userType.equals("passenger")) {
            Passenger passenger = passengerRepository.findByEmail(email);
            if (passenger == null) {
                throw new RuntimeException("Email not found");
            }
            passenger.setPassword(newPassword);
            passengerRepository.save(passenger);
        } else {
            Driver driver = driverRepository.findByEmail(email);
            if (driver == null) {
                throw new RuntimeException("Email not found");
            }
            driver.setPassword(newPassword);
            driverRepository.save(driver);
        }
    }
    
    public void fundWallet(Long passengerId, double amount) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));
        
        if (amount <= 0) {
            throw new RuntimeException("Amount must be greater than 0");
        }
        
        passenger.setWalletBalance(passenger.getWalletBalance() + amount);
        passengerRepository.save(passenger);
    }
    
    public double getWalletBalance(Long passengerId) {
        Passenger passenger = passengerRepository.findById(passengerId)
                .orElseThrow(() -> new RuntimeException("Passenger not found"));
        return passenger.getWalletBalance();
    }
    
    public double getDriverEarnings(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        return driver.getEarnings();
    }
    
    public double getDriverRating(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        return driver.getRating();
    }
}