package com.tev.riderapp.rideshare.repository;

import com.tev.riderapp.model.Payment;
import com.tev.riderapp.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByRide(Ride ride);
    List<Payment> findByRidePassengerId(Long passengerId);
}