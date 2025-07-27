package com.tev.riderapp.service;

import com.tev.riderapp.model.Driver;
import com.tev.riderapp.model.Location;
import com.tev.riderapp.rideshare.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverService {
    private final DriverRepository driverRepository;

    public List<Driver> getNearbyDrivers(Location location, double radiusKm) {
        List<Driver> availableDrivers = driverRepository.findByAvailableTrue();
        
        return availableDrivers.stream()
                .filter(driver -> {
                    if (driver.getCurrentLocation() == null) return false;
                    double distance = calculateDistance(location, driver.getCurrentLocation());
                    return distance <= radiusKm;
                })
                .collect(Collectors.toList());
    }

    public void updateLocation(Long driverId, Location location) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        driver.setCurrentLocation(location);
        driverRepository.save(driver);
    }

    public void updateAvailability(Long driverId, boolean available) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        driver.setAvailable(available);
        driverRepository.save(driver);
    }

    public Double getEarnings(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        return driver.getEarnings();
    }

    public Double getRating(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver not found"));
        return driver.getRating();
    }

    private double calculateDistance(Location loc1, Location loc2) {
        final int R = 6371; // Earth's radius in kilometers

        double lat1 = Math.toRadians(loc1.getLatitude());
        double lon1 = Math.toRadians(loc1.getLongitude());
        double lat2 = Math.toRadians(loc2.getLatitude());
        double lon2 = Math.toRadians(loc2.getLongitude());

        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;

        double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}