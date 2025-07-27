package com.tev.riderapp.rideshare.repository;

import com.tev.riderapp.model.Driver;
import com.tev.riderapp.model.Passenger;
import com.tev.riderapp.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RideRepository extends JpaRepository<Ride, Long> {
    List<Ride> findByPassenger(Passenger passenger);
    List<Ride> findByDriver(Driver driver);
    List<Ride> findByStatus(Ride.RideStatus status);
}