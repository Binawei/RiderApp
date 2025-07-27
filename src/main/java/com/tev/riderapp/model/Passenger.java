package com.tev.riderapp.model;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Passenger extends User {
    @OneToMany(mappedBy = "passenger")
    private List<Ride> rides = new ArrayList<>();
    private double walletBalance;
}