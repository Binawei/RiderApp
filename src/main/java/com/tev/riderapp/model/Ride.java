package com.tev.riderapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Passenger passenger;

    @ManyToOne
    private Driver driver;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "pickup_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "pickup_longitude")),
        @AttributeOverride(name = "address", column = @Column(name = "pickup_address")),
        @AttributeOverride(name = "postcode", column = @Column(name = "pickup_postcode"))
    })
    private Location pickupLocation;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "dropoff_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "dropoff_longitude")),
        @AttributeOverride(name = "address", column = @Column(name = "dropoff_address")),
        @AttributeOverride(name = "postcode", column = @Column(name = "dropoff_postcode"))
    })
    private Location dropoffLocation;

    private LocalDateTime requestTime;
    private LocalDateTime pickupTime;
    private LocalDateTime dropoffTime;
    private RideStatus status;
    private RideType rideType;
    private double fare;
    private double distance;
    private int rating;
    private double surgeMultiplier;
    private String paymentMethod;

    public enum RideStatus {
        REQUESTED, ACCEPTED, PICKED_UP, COMPLETED, CANCELLED
    }

    public enum RideType {
        STANDARD, POOL, LUXURY
    }

}