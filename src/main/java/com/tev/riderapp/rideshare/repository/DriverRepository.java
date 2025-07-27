package com.tev.riderapp.rideshare.repository;

import com.tev.riderapp.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Driver findByEmail(String email);
    List<Driver> findByAvailableTrue();
}