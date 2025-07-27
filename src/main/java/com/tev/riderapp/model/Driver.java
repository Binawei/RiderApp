package com.tev.riderapp.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Driver extends User {
    @OneToMany(mappedBy = "driver")
    private List<Ride> rides = new ArrayList<>();
    private String vehicleNumber;
    private String vehicleType;
    private boolean available;
    private double rating;
    private double earnings;
    private Integer totalRides = 0;
    private Location currentLocation;

    @Embedded
    public Location getCurrentLocation() {
        return currentLocation;
    }

}